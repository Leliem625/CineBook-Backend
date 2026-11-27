package com.cinebook.ticket.service;

/**
 * Sinh token QR / link vé (FR-404). TODO ký và xác thực bằng HMAC khi làm check-in.
 */
public interface QrTokenService {

    /** Token mới: 64 ký tự hex từ 32 byte ngẫu nhiên an toàn, không đoán được. */
    String newToken();
}
