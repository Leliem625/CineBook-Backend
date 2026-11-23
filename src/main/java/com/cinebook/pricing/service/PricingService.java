package com.cinebook.pricing.service;

import java.time.LocalDate;

import com.cinebook.cinema.entity.ScreenFormat;
import com.cinebook.cinema.entity.SeatType;

/**
 * Tính giá ghế tại thời điểm giữ (BR-11, BR-12, BR-13). Sơ đồ ghế, danh sách suất chiếu và booking
 * đều dùng chung service này để giá khách thấy luôn bằng giá bị chốt vào đơn.
 */
public interface PricingService {

    /**
     * Giá một ghế (VND nguyên, đã làm tròn lên 1.000đ).
     *
     * @param date ngày chiếu theo giờ Việt Nam (quyết định giá cuối tuần)
     */
    long priceOf(ScreenFormat format, SeatType seatType, LocalDate date);
}
