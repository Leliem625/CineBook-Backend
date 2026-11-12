package com.cinebook.promotion.repository;

import com.cinebook.promotion.entity.Voucher;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VoucherRepository extends JpaRepository<Voucher, Long> {
}
