package com.cinebook.booking.service;

import com.cinebook.booking.dto.HoldRequest;
import com.cinebook.booking.dto.HoldResponse;

/**
 * Giữ ghế nguyên tử (BR-01..09).
 */
public interface SeatHoldService {

    /**
     * Giữ ghế cho một phiên khách và tạo booking HELD. Nếu phiên đã có booking HELD ở suất này thì
     * nhả ghế cũ và huỷ booking cũ trước (chọn lại ghế).
     * <p>
     * Lỗi: id suất chiếu không tồn tại 404; quá số ghế, ghế không thuộc phòng, ghế đôi lẻ cặp
     * {@code VALIDATION_FAILED}; suất chưa/ngừng bán {@code SHOWTIME_NOT_AVAILABLE};
     * ghế đã có người giữ {@code SEAT_UNAVAILABLE} kèm {@code seatIds} bị mất (rollback toàn bộ, BR-03).
     *
     * @param sessionId id phiên khách lấy từ cookie
     */
    HoldResponse hold(String sessionId, HoldRequest request);

    /**
     * Khách bỏ giữ: nhả ghế và chuyển đơn HELD sang CANCELLED (FR-312).
     * <p>
     * Idempotent: đơn đã CANCELLED hoặc EXPIRED thì không làm gì. Đơn không tồn tại hoặc không thuộc
     * phiên này trả {@code BOOKING_NOT_FOUND} (404, cố ý không phân biệt để không lộ đơn của người khác).
     * Đơn đã sang bước thanh toán trở đi trả {@code CANCEL_NOT_ALLOWED}.
     */
    void release(String sessionId, Long bookingId);
}
