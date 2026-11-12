package com.cinebook.common.idempotency;

/**
 * Xử lý header Idempotency-Key (BR-10).
 */
public interface IdempotencyService {

    // TODO Lưu key -> kết quả trong Redis (TTL 24h); trả lại kết quả cũ khi gửi trùng
}
