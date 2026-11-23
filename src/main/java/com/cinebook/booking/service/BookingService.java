package com.cinebook.booking.service;

import com.cinebook.booking.dto.BookingResponse;
import com.cinebook.booking.dto.PaymentTarget;

/**
 * Xem đơn; sau này: cập nhật combo, voucher, thông tin khách, tính tổng tiền.
 */
public interface BookingService {

    /**
     * Chi tiết đơn của phiên khách. Đơn không tồn tại hoặc không thuộc phiên này đều trả
     * {@code BOOKING_NOT_FOUND} (404), không phân biệt để không lộ đơn của người khác.
     */
    BookingResponse getBooking(String sessionId, Long bookingId);

    /**
     * Chuyển đơn HELD sang PAYMENT_PENDING để bắt đầu thanh toán. Phải gọi trong transaction của người gọi
     * để lỗi ở bước sau (tạo payment, dựng URL) làm rollback luôn việc đổi trạng thái.
     * <p>
     * Idempotent: đơn đã PAYMENT_PENDING thì trả lại với {@code alreadyPending = true}.
     * Lỗi: đơn không có hoặc của phiên khác {@code BOOKING_NOT_FOUND} (404); hết hạn giữ ghế
     * {@code HOLD_EXPIRED} (410); đã huỷ hoặc đã thanh toán {@code CONFLICT} (409).
     */
    PaymentTarget beginPayment(String sessionId, Long bookingId);
}
