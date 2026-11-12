package com.cinebook.iam.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Tài khoản admin / manager / staff.
 */
@Entity
@Table(name = "staff_account")
public class StaffAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // TODO fields: username, passwordHash, role, chainId, cinemaId, enabled
}
