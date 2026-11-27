package com.cinebook.payment.dto;

import com.cinebook.booking.dto.BookingResponse;
import com.cinebook.payment.entity.PaymentStatus;

/** Kết quả một lần thanh toán cho trang kết quả. Chỉ tin dữ liệu này, không tin tham số trên URL (BR-25). */
public record PaymentResultResponse(PaymentStatus paymentStatus, BookingResponse booking) {
}
