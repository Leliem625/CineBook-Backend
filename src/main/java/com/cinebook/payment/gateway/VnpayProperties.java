package com.cinebook.payment.gateway;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Cấu hình VNPay (application.yml: cinebook.vnpay.*). {@code tmnCode} và {@code hashSecret} đọc từ
 * biến môi trường VNPAY_TMN_CODE / VNPAY_HASH_SECRET trong .env, không commit.
 */
@ConfigurationProperties(prefix = "cinebook.vnpay")
public record VnpayProperties(String tmnCode, String hashSecret, String payUrl, String returnUrl) {

    public boolean isConfigured() {
        return tmnCode != null && !tmnCode.isBlank() && hashSecret != null && !hashSecret.isBlank();
    }
}
