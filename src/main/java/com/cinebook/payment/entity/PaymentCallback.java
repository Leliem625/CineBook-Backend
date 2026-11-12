package com.cinebook.payment.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Mọi callback nhận được (raw) để đối soát (FR-503).
 */
@Entity
@Table(name = "payment_callback")
public class PaymentCallback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // TODO fields: provider, payload, signatureValid, receivedAt
}
