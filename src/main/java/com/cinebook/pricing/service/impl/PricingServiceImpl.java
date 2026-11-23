package com.cinebook.pricing.service.impl;

import java.time.DayOfWeek;
import java.time.LocalDate;

import org.springframework.stereotype.Service;

import com.cinebook.cinema.entity.ScreenFormat;
import com.cinebook.cinema.entity.SeatType;
import com.cinebook.pricing.service.PricingService;

/**
 * Cài đặt {@link PricingService} bằng bảng giá TẠM THỜI: giá cơ bản theo định dạng, cộng thêm theo loại ghế,
 * cuối tuần +10%, làm tròn lên 1.000đ.
 * TODO đọc bảng giá thật (price_rule, holiday, khung giờ) khi có.
 */
@Service
public class PricingServiceImpl implements PricingService {

    @Override
    public long priceOf(ScreenFormat format, SeatType seatType, LocalDate date) {
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
