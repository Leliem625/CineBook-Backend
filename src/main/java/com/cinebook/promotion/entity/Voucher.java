package com.cinebook.promotion.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Voucher (BR-14, BR-18).
 */
@Entity
@Table(name = "voucher")
public class Voucher {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // TODO fields: code, discountType, discountValue, maxDiscount, minOrder, validFrom, validTo, totalLimit, usedCount, reservedCount, perContactLimit, scope
}
