package com.cinebook.booking.dto;

import java.time.Instant;
import java.util.List;

import com.cinebook.booking.entity.BookingStatus;
import com.cinebook.cinema.entity.SeatType;

/**
 * Chi tiết một đơn cho trang thanh toán / kết quả. {@code serverNow} để frontend tính đồng hồ đếm ngược
 * bằng {@code holdExpiresAt - serverNow}. Đơn HELD có {@code holdExpiresAt} đã qua nghĩa là hết hạn
 * (job nhả ghế có thể chạy trễ vài chục giây).
 */
public record BookingResponse(
        Long id,
        String code,
        BookingStatus status,
        Long showtimeId,
        List<Seat> seats,
        long subtotal,
        long discount,
        long total,
        String customerName,
        String email,
        String phone,
        Instant holdExpiresAt,
        Instant serverNow) {

    /** Ghế trong đơn: nhãn để hiển thị (A1, A2...) và giá đã chốt. */
    public record Seat(Long seatId, String row, int number, SeatType type, long price) {
    }
}
