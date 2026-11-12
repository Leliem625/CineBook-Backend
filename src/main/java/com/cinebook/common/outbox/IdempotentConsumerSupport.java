package com.cinebook.common.outbox;

import org.springframework.stereotype.Service;

/**
 * Hỗ trợ consumer bỏ qua event đã xử lý.
 */
@Service
public class IdempotentConsumerSupport {

    // TODO runOnce(consumer, eventId, action) - insert processed_event + action trong cùng transaction
}
