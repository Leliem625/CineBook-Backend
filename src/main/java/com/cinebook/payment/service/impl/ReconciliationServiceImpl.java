package com.cinebook.payment.service.impl;

import java.time.Duration;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cinebook.booking.service.BookingConfirmationService;
import com.cinebook.payment.entity.Payment;
import com.cinebook.payment.entity.PaymentStatus;
import com.cinebook.payment.repository.PaymentRepository;
import com.cinebook.payment.service.ReconciliationService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Cài đặt {@link ReconciliationService}. Mốc quá hạn = {@code cinebook.payment.pending-timeout} (mặc định 15 phút)
 * kể từ lúc tạo payment. Hạn trên cổng bằng hạn giữ ghế (tối đa 10 phút) nên 15 phút còn dư vài phút cho IPN đến
 * chậm: khách thanh toán sát giờ vẫn kịp được xác nhận thay vì bị nhả ghế oan.
 * IPN thành công đến SAU khi payment đã bị đóng ở đây được {@code PaymentServiceImpl.handleIpn} ghi nhận để hoàn tiền.
 */
@Slf4j
@Service
public class ReconciliationServiceImpl implements ReconciliationService {

    /** Mã phản hồi ghi vào payment bị đóng do quá hạn; handleIpn dựa vào mã này để nhận ra thanh toán muộn. */
    public static final String TIMEOUT_CODE = "TIMEOUT";

    private final PaymentRepository paymentRepository;
    private final BookingConfirmationService confirmationService;
    private final long timeoutSeconds;

    public ReconciliationServiceImpl(PaymentRepository paymentRepository,
                                     BookingConfirmationService confirmationService,
                                     @Value("${cinebook.payment.pending-timeout:15m}") Duration pendingTimeout) {
        this.paymentRepository = paymentRepository;
        this.confirmationService = confirmationService;
        this.timeoutSeconds = pendingTimeout.toSeconds();
    }

    @Override
    @Transactional
    public int expireStalePayments(int batchSize) {
        List<Long> ids = paymentRepository.lockStalePending(timeoutSeconds, batchSize);
        for (Payment payment : paymentRepository.findAllById(ids)) {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setResponseCode(TIMEOUT_CODE);
            confirmationService.expireUnpaid(payment.getBookingId());
            log.info("Payment {} quá hạn {}s không có kết quả: đóng payment, nhả ghế đơn {}",
                    payment.getTxnRef(), timeoutSeconds, payment.getBookingId());
        }
        return ids.size();
    }
}
