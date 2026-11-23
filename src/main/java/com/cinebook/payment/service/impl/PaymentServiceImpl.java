package com.cinebook.payment.service.impl;

import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cinebook.booking.dto.PaymentTarget;
import com.cinebook.booking.service.BookingService;
import com.cinebook.common.exception.BusinessException;
import com.cinebook.common.exception.ErrorCode;
import com.cinebook.payment.dto.CreatePaymentRequest;
import com.cinebook.payment.dto.PaymentResponse;
import com.cinebook.payment.entity.Payment;
import com.cinebook.payment.entity.PaymentProvider;
import com.cinebook.payment.entity.PaymentStatus;
import com.cinebook.payment.gateway.PaymentGateway;
import com.cinebook.payment.gateway.PaymentUrlRequest;
import com.cinebook.payment.repository.PaymentRepository;
import com.cinebook.payment.service.PaymentService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Cài đặt {@link PaymentService}. Toàn bộ trong một transaction: đổi trạng thái đơn, tạo payment và dựng URL
 * thành công cùng nhau hoặc rollback cùng nhau (URL lỗi thì đơn vẫn HELD, khách bấm lại được).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final BookingService bookingService;
    private final PaymentRepository paymentRepository;
    private final PaymentGateway gateway;

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
}
