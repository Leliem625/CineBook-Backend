package com.cinebook.payment.gateway;

import java.time.Instant;

/**
 * Dữ liệu để dựng URL thanh toán.
 *
 * @param txnRef    mã tham chiếu CineBook sinh ra, duy nhất cho mỗi lần thanh toán
 * @param amountVnd số tiền VND nguyên (cổng tự nhân 100 theo quy ước của VNPay)
 * @param orderInfo nội dung thanh toán, chỉ ký tự ASCII không dấu (VD "Thanh toan ve CineBook ABC12345")
 * @param ipAddress IP của khách (local dùng 127.0.0.1)
 * @param createdAt thời điểm tạo
 * @param expiresAt hạn thanh toán; quá hạn cổng từ chối giao dịch
 */
public record PaymentUrlRequest(String txnRef, long amountVnd, String orderInfo, String ipAddress,
                                Instant createdAt, Instant expiresAt) {
}
