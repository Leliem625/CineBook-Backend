package com.cinebook.promotion.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Lượt dùng voucher theo booking.
 */
@Entity
@Table(name = "voucher_redemption")
public class VoucherRedemption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // TODO fields: voucher, booking, email, phone, status
}
