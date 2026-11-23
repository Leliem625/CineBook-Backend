package com.cinebook.payment.dto;

import com.cinebook.payment.entity.PaymentProvider;

import jakarta.validation.constraints.NotNull;

/** Yêu cầu bắt đầu thanh toán một đơn. Hiện chỉ hỗ trợ VNPAY. */
public record CreatePaymentRequest(@NotNull(message = "Chưa chọn cổng thanh toán") PaymentProvider provider) {
}
