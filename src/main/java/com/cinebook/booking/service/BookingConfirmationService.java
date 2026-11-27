package com.cinebook.booking.service;

/**
 * Kết quả thanh toán áp lên đơn: CONFIRMED, ghế SOLD, sinh vé (khi thành công) hoặc trả về HELD (khi thất bại).
 * TODO voucher used (khi có voucher).
 */
public interface BookingConfirmationService {

    /**
     * Xác nhận đơn đã thanh toán: PAYMENT_PENDING -> CONFIRMED, ghế HELD -> SOLD, sinh vé. Phải gọi trong
     * transaction của người gọi: lỗi giữa chừng thì rollback tất cả.
     *
     * @return false nếu đơn không còn PAYMENT_PENDING (trả tiền muộn): không làm gì, người gọi xử lý hoàn tiền
     * @throws IllegalStateException nếu số ghế bán được khác số ghế của đơn
     */
    boolean confirm(Long bookingId);

    /** Thanh toán thất bại: PAYMENT_PENDING -> HELD để khách thử lại trong thời gian giữ ghế còn lại. */
    void paymentFailed(Long bookingId);

    /**
     * Quá hạn thanh toán mà không có kết quả: PAYMENT_PENDING -> EXPIRED và nhả ghế. Không làm gì nếu đơn
     * không còn PAYMENT_PENDING (đã được xác nhận hoặc đã xử lý).
     */
    void expireUnpaid(Long bookingId);
}
