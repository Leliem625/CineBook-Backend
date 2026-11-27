package com.cinebook.payment.service;

/**
 * Đối soát payment PENDING quá hạn (FR-505): khách đóng tab ở trang cổng hoặc mất IPN thì đơn không được kẹt
 * ở PAYMENT_PENDING mãi với ghế bị giữ.
 */
public interface ReconciliationService {

    /**
     * Xử lý một lô tối đa {@code batchSize} payment PENDING quá hạn trong một transaction: payment thành FAILED
     * (mã TIMEOUT), đơn thành EXPIRED và nhả ghế. TODO hỏi cổng (queryTransaction) trước khi kết luận thất bại.
     *
     * @return số payment đã xử lý; bằng {@code batchSize} nghĩa là có thể còn nữa, gọi lại
     */
    int expireStalePayments(int batchSize);
}
