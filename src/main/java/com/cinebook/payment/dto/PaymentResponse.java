package com.cinebook.payment.dto;

import com.cinebook.payment.entity.PaymentProvider;

/**
 * Kết quả tạo thanh toán. Frontend chuyển trình duyệt của khách tới {@code payUrl}
 * (không redirect bằng HTTP vì request là fetch POST).
 */
public record PaymentResponse(Long paymentId, PaymentProvider provider, String payUrl) {
}
