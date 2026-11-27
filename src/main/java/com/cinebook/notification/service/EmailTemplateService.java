package com.cinebook.notification.service;

import com.cinebook.booking.dto.BookingResponse;
import com.cinebook.notification.dto.EmailContent;
import com.cinebook.payment.event.PaymentSucceededEvent;
import com.cinebook.showtime.dto.SeatMapResponse;

/**
 * Dựng nội dung email: vé, OTP, hủy, hoàn tiền.
 */
public interface EmailTemplateService {

    /** Email xác nhận thanh toán và thông tin vé: mã đặt vé, phim, rạp, giờ chiếu, ghế kèm mã vé, thanh toán. */
    EmailContent ticketConfirmation(BookingResponse booking, SeatMapResponse showtime, PaymentSucceededEvent payment);
}
