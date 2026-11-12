package com.cinebook.booking.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Đơn đặt vé.
 */
@Entity
@Table(name = "booking")
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // TODO fields: code, guestSessionId, userId (nullable - M10), customerName, email, phone, showtime, status, holdExpiresAt, subtotal, discount, total, voucherId, idempotencyKey, version
}
