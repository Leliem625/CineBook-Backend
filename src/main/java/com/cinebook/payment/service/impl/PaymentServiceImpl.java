package com.cinebook.payment.service.impl;

import java.time.Instant;
import java.util.Map;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cinebook.booking.dto.BookingResponse;
import com.cinebook.booking.dto.PaymentTarget;
import com.cinebook.booking.service.BookingConfirmationService;
import com.cinebook.booking.service.BookingService;
import com.cinebook.common.exception.BusinessException;
import com.cinebook.common.exception.ErrorCode;
import com.cinebook.payment.dto.CreatePaymentRequest;
import com.cinebook.payment.dto.IpnResponse;
import com.cinebook.payment.dto.PaymentResponse;
import com.cinebook.payment.dto.PaymentResultResponse;
import com.cinebook.payment.entity.CallbackType;
import com.cinebook.payment.entity.Payment;
import com.cinebook.payment.entity.PaymentProvider;
import com.cinebook.payment.entity.PaymentStatus;
import com.cinebook.payment.event.PaymentSucceededEvent;
import com.cinebook.payment.gateway.PaymentGateway;
import com.cinebook.payment.gateway.PaymentUrlRequest;
import com.cinebook.payment.repository.PaymentRepository;
import com.cinebook.payment.service.PaymentCallbackRecorder;
import com.cinebook.payment.service.PaymentService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Cài đặt {@link PaymentService}. Mỗi phương thức chạy trong một transaction: các thay đổi (trạng thái đơn,
 * payment, ghế, vé) thành công cùng nhau hoặc rollback cùng nhau.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final BookingService bookingService;
    private final BookingConfirmationService confirmationService;
    private final PaymentRepository paymentRepository;
    private final PaymentCallbackRecorder callbackRecorder;
    private final PaymentGateway gateway;
    private final ApplicationEventPublisher events;

    // ------------------------------------------------------------------ tạo thanh toán

    @Override
    @Transactional
    public PaymentResponse createPayment(String sessionId, Long bookingId, CreatePaymentRequest request,
                                         String ipAddress) {
        if (request.provider() != PaymentProvider.VNPAY) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED,
                    "Chưa hỗ trợ thanh toán qua " + request.provider());
        }

        PaymentTarget target = bookingService.beginPayment(sessionId, bookingId);
        Payment payment = target.alreadyPending()
                ? paymentRepository.findFirstByBookingIdAndStatusOrderByIdDesc(bookingId, PaymentStatus.PENDING)
                        .orElseGet(() -> newPayment(target))
                : newPayment(target);

        String payUrl;
        try {
            // Hạn trên cổng = hạn giữ ghế: không cho trả tiền sau khi đồng hồ giữ ghế đã hết
            payUrl = gateway.createPaymentUrl(new PaymentUrlRequest(payment.getTxnRef(), payment.getAmount(),
                    "Thanh toan ve CineBook " + target.code(), ipAddress, payment.getCreatedAt(),
                    target.holdExpiresAt()));
        } catch (IllegalStateException e) {
            log.warn("Không dựng được URL thanh toán: {}", e.getMessage());
            throw new BusinessException(ErrorCode.PAYMENT_UNAVAILABLE, "Cổng thanh toán chưa sẵn sàng");
        }
        return new PaymentResponse(payment.getId(), payment.getProvider(), payUrl);
    }

    /** Mã tham chiếu = mã đơn + mốc thời gian: duy nhất, chỉ gồm chữ và số (VNPay giới hạn ký tự). */
    private Payment newPayment(PaymentTarget target) {
        Payment payment = new Payment();
        payment.setBookingId(target.bookingId());
        payment.setProvider(PaymentProvider.VNPAY);
        payment.setTxnRef(target.code() + Instant.now().toEpochMilli());
        payment.setAmount(target.total());
        payment.setStatus(PaymentStatus.PENDING);
        return paymentRepository.save(payment);
    }

    // ------------------------------------------------------------------ IPN

    @Override
    @Transactional
    public IpnResponse handleIpn(Map<String, String> params) {
        String txnRef = params.get("vnp_TxnRef");

        boolean signatureValid;
        try {
            signatureValid = gateway.verifyCallback(params);
        } catch (IllegalStateException e) { // cổng chưa cấu hình khóa nên không kiểm tra được
            log.error("Không kiểm tra được chữ ký IPN: {}", e.getMessage());
            return IpnResponse.UNKNOWN_ERROR;
        }

        // Ghi callback TRƯỚC khi khóa dòng payment (xem PaymentCallbackRecorder); chỉ lấy id, không nạp entity
        Long paymentId = txnRef == null ? null : paymentRepository.findIdByTxnRef(txnRef).orElse(null);
        callbackRecorder.record(CallbackType.IPN, txnRef, params, signatureValid, paymentId);

        if (!signatureValid) {
            return IpnResponse.INVALID_SIGNATURE; // không tin bất kỳ dữ liệu nào trong callback (BR-21)
        }
        if (paymentId == null) {
            return IpnResponse.ORDER_NOT_FOUND;
        }

        // Khóa dòng payment: hai IPN trùng nhau xử lý lần lượt, lần sau thấy payment đã xử lý rồi
        Payment payment = paymentRepository.findByTxnRefForUpdate(txnRef).orElseThrow();

        if (!amountMatches(params.get("vnp_Amount"), payment.getAmount())) {
            if (payment.getStatus() == PaymentStatus.PENDING) {
                payment.setStatus(PaymentStatus.AMOUNT_MISMATCH);
                log.error("IPN {} sai số tiền: nhận {}, cần {}", txnRef, params.get("vnp_Amount"), payment.getAmount() * 100);
                confirmationService.paymentFailed(payment.getBookingId()); // đơn không kẹt, khách thử lại được
            }
            return IpnResponse.INVALID_AMOUNT;
        }

        String responseCode = params.get("vnp_ResponseCode");
        String transactionStatus = params.get("vnp_TransactionStatus");
        boolean success = "00".equals(responseCode) && (transactionStatus == null || "00".equals(transactionStatus));

        if (success && payment.getStatus() == PaymentStatus.FAILED
                && ReconciliationServiceImpl.TIMEOUT_CODE.equals(payment.getResponseCode())) {
            // Thanh toán thành công đến muộn, sau khi job đối soát đã đóng payment và nhả ghế: tiền đã nhận
            // nhưng đơn không còn. Ghi nhận để hoàn tiền (TODO LatePaymentService: bán lại nếu ghế còn).
            payment.setStatus(PaymentStatus.REFUND_REQUESTED);
            payment.setProviderTxnId(params.get("vnp_TransactionNo"));
            payment.setPaidAt(Instant.now());
            log.error("Payment {} thành công nhưng đến sau khi đã quá hạn và nhả ghế: cần hoàn tiền", txnRef);
            return IpnResponse.SUCCESS;
        }
        if (payment.getStatus() != PaymentStatus.PENDING) {
            return IpnResponse.ALREADY_CONFIRMED; // idempotent: IPN gửi lại không làm gì
        }

        payment.setResponseCode(responseCode);
        if (success) {
            onSuccess(payment, params.get("vnp_TransactionNo"));
        } else {
            onFailure(payment, responseCode);
        }
        return IpnResponse.SUCCESS;
    }

    /** vnp_Amount = số tiền VND nhân 100. */
    private static boolean amountMatches(String vnpAmount, long amountVnd) {
        try {
            return vnpAmount != null && Long.parseLong(vnpAmount) == amountVnd * 100;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private void onSuccess(Payment payment, String providerTxnId) {
        payment.setProviderTxnId(providerTxnId);
        payment.setPaidAt(Instant.now());
        if (confirmationService.confirm(payment.getBookingId())) {
            payment.setStatus(PaymentStatus.SUCCEEDED);
            // Chỉ được xử lý SAU KHI transaction này commit (xem NotificationEventConsumer): rollback thì không gửi gì
            events.publishEvent(new PaymentSucceededEvent(payment.getBookingId(), payment.getProvider(),
                    payment.getTxnRef(), providerTxnId, payment.getAmount(), payment.getPaidAt()));
        } else {
            // Tiền đã nhận nhưng đơn không còn chờ thanh toán (trả muộn): ghi nhận để hoàn tiền sau
            // TODO LatePaymentService: bán lại nếu ghế còn, ngược lại hoàn tiền
            payment.setStatus(PaymentStatus.REFUND_REQUESTED);
            log.error("Payment {} thành công nhưng đơn {} không còn PAYMENT_PENDING: cần hoàn tiền",
                    payment.getTxnRef(), payment.getBookingId());
        }
    }

    private void onFailure(Payment payment, String responseCode) {
        // Mã 24: khách tự huỷ giao dịch
        payment.setStatus("24".equals(responseCode) ? PaymentStatus.CANCELLED_BY_USER : PaymentStatus.FAILED);
        confirmationService.paymentFailed(payment.getBookingId());
    }

    // ------------------------------------------------------------------ kết quả

    @Override
    @Transactional(readOnly = true)
    public PaymentResultResponse getResult(String sessionId, String txnRef) {
        Payment payment = paymentRepository.findByTxnRef(txnRef)
                .orElseThrow(() -> new BusinessException(ErrorCode.BOOKING_NOT_FOUND, "Không tìm thấy giao dịch"));
        // getBooking kiểm tra đơn thuộc phiên này, nếu không thì 404 (không lộ giao dịch của người khác)
        BookingResponse booking = bookingService.getBooking(sessionId, payment.getBookingId());
        return new PaymentResultResponse(payment.getStatus(), booking);
    }
}
