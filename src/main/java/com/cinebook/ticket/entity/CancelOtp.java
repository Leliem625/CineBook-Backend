package com.cinebook.ticket.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * OTP hủy vé (FR-403).
 */
@Entity
@Table(name = "cancel_otp")
public class CancelOtp {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // TODO fields: booking, otpHash, expiresAt, attempts
}
