package com.cinebook.notification.event;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.cinebook.booking.dto.BookingResponse;
import com.cinebook.booking.service.BookingService;
import com.cinebook.notification.dto.EmailContent;
import com.cinebook.notification.service.EmailService;
import com.cinebook.notification.service.EmailTemplateService;
import com.cinebook.payment.event.PaymentSucceededEvent;
import com.cinebook.showtime.dto.SeatMapResponse;
import com.cinebook.showtime.service.SeatMapService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Nghe sự kiện nghiệp vụ và gửi email (FR-601, FR-602). Hiện nghe sự kiện trong tiến trình;
 * khi có outbox/Kafka sẽ đổi sang nhận booking.confirmed, otp.requested, refund.*, showtime.cancelled.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventConsumer {

    private final BookingService bookingService;
    private final SeatMapService seatMapService;
    private final EmailTemplateService templates;
    private final EmailService emailService;

    /**
     * Gửi email vé + thanh toán sau khi thanh toán thành công.
     * {@code AFTER_COMMIT}: chỉ gửi khi dữ liệu đã chắc chắn được lưu (rollback thì không gửi gì).
     * {@code @Async}: SMTP chậm hay lỗi không làm chậm hay hỏng việc trả lời IPN cho cổng thanh toán.
     * Gửi không được thì chỉ ghi log, chưa có thử lại (TODO: cột email_sent_at + job gửi lại).
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPaymentSucceeded(PaymentSucceededEvent event) {
        try {
            BookingResponse booking = bookingService.getBookingForSystem(event.bookingId());
            if (booking.email() == null || booking.email().isBlank()) {
                log.warn("Đơn {} không có email, bỏ qua việc gửi vé", booking.code());
                return;
            }
            SeatMapResponse showtime = seatMapService.getSeatMap(booking.showtimeId());
            EmailContent content = templates.ticketConfirmation(booking, showtime, event);
            emailService.send(booking.email(), content);
        } catch (Exception e) {
            log.error("Không dựng được email vé cho đơn id={}", event.bookingId(), e);
        }
    }
}
