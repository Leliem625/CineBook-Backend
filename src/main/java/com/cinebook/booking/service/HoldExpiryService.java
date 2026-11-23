package com.cinebook.booking.service;

/**
 * Hết hạn giữ ghế: EXPIRED, nhả ghế (BR-05). TODO trả lượt voucher khi có voucher.
 */
public interface HoldExpiryService {

    /**
     * Xử lý một lô tối đa {@code batchSize} đơn HELD đã quá hạn trong một transaction:
     * nhả ghế, chuyển sang EXPIRED, ghi history.
     *
     * @return số đơn đã xử lý; bằng {@code batchSize} nghĩa là có thể còn nữa, gọi lại
     */
    int expireOverdue(int batchSize);
}
