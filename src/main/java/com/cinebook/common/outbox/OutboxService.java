package com.cinebook.common.outbox;

/**
 * Ghi event vào outbox trong cùng transaction nghiệp vụ.
 */
public interface OutboxService {

    // TODO save(aggregateType, aggregateId, eventType, payload) - gọi bên trong @Transactional
    // TODO Giai đoạn 4: Debezium đọc bảng outbox_event và đẩy lên Kafka
}
