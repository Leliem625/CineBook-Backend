package com.cinebook.showtime.service.impl;

import com.cinebook.cinema.entity.ScreenFormat;
import com.cinebook.cinema.entity.SeatType;

import java.time.DayOfWeek;
import java.time.LocalDate;

/**
 * Bảng giá TẠM THỜI (BR-11, BR-13): giá cơ bản theo định dạng, cộng thêm theo loại ghế,
 * cuối tuần +10%, làm tròn lên 1.000đ. Số tiền là VND nguyên.
 * TODO thay bằng PricingService khi có bảng giá thật (ngày lễ, khung giờ, cấu hình theo rạp).
 */
final class ShowtimePricing {

    private ShowtimePricing() {
    }

    /** Giá ghế thường, dùng làm "giá từ" của suất chiếu. */
    static long fromPrice(ScreenFormat format, LocalDate date) {
        return price(format, SeatType.STANDARD, date);
    }

    static long price(ScreenFormat format, SeatType seatType, LocalDate date) {
        long base = switch (format) {
            case F2D -> 65_000;
            case F3D -> 85_000;
            case IMAX -> 120_000;
            case F4DX -> 130_000;
        };
        base += switch (seatType) {
            case STANDARD -> 0;
            case VIP -> 15_000;
            case COUPLE -> 30_000; // giá mỗi ghế trong cặp
        };
        boolean weekend = date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY;
        long price = weekend ? base * 110 / 100 : base;
        return (price + 999) / 1000 * 1000;
    }
}
