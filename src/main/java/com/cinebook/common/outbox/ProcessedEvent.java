package com.cinebook.common.outbox;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Đánh dấu event đã xử lý (consumer idempotent).
 */
@Entity
@Table(name = "processed_event")
public class ProcessedEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // TODO fields: consumer, eventId (khóa chính ghép), processedAt
}
