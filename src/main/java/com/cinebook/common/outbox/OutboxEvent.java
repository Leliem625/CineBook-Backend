package com.cinebook.common.outbox;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Sự kiện chờ đẩy lên Kafka (Transactional Outbox).
 */
@Entity
@Table(name = "outbox_event")
public class OutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // TODO fields: eventId (UUID), aggregateType, aggregateId, eventType, payload (JSONB), createdAt
}
