package com.cinebook.payment.repository;

import com.cinebook.payment.entity.PaymentCallback;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentCallbackRepository extends JpaRepository<PaymentCallback, Long> {
}
