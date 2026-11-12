package com.cinebook.common.outbox;

import org.springframework.stereotype.Service;

/**
 * Ghi event vào outbox trong cùng transaction nghiệp vụ.
 */
@Service
public class OutboxService {

    // TODO save(aggregateType, aggregateId, eventType, payload) - gọi bên trong @Transactional
    // TODO Giai đoạn 4: Debezium đọc bảng outbox_event và đẩy lên Kafka
}
