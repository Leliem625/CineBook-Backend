package com.cinebook.booking.dto;

import java.time.Instant;
import java.util.List;

/**
 * Kết quả giữ ghế thành công.
 *
 * @param bookingId     id đơn vừa tạo (trạng thái HELD)
 * @param bookingCode   mã đơn hiển thị cho khách
 * @param showtimeId    suất chiếu
 * @param seatIds       các ghế đã giữ, sắp xếp tăng dần
 * @param holdExpiresAt thời điểm hết hạn giữ ghế
 * @param serverNow     giờ server lúc trả lời: frontend tính đồng hồ đếm ngược bằng
 *                      {@code holdExpiresAt - serverNow} để không phụ thuộc đồng hồ máy khách
 */
public record HoldResponse(
        Long bookingId,
        String bookingCode,
        Long showtimeId,
        List<Long> seatIds,
        Instant holdExpiresAt,
        Instant serverNow) {
}
