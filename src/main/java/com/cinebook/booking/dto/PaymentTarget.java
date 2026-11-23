package com.cinebook.booking.dto;

import java.time.Instant;

/**
 * Thông tin booking mà module payment cần để tạo thanh toán.
 *
 * @param alreadyPending true nếu đơn đã ở PAYMENT_PENDING từ lần bấm trước (không phải vừa chuyển)
 */
public record PaymentTarget(Long bookingId, String code, long total, Instant holdExpiresAt, boolean alreadyPending) {
}
