package com.cinebook.pricing.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Quy tắc giá (BR-11).
 */
@Entity
@Table(name = "price_rule")
public class PriceRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // TODO fields: chain, seatType, format, dayType, timeSlot, basePrice, multiplier
}
