package com.cinebook.common.idempotency;

import org.springframework.stereotype.Service;

/**
 * Xử lý header Idempotency-Key (BR-10).
 */
@Service
public class IdempotencyService {

    // TODO Lưu key -> kết quả trong Redis (TTL 24h); trả lại kết quả cũ khi gửi trùng
}
