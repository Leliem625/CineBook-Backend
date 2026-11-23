package com.cinebook.payment.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cinebook.payment.entity.Payment;
import com.cinebook.payment.entity.PaymentStatus;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    /** Lần thanh toán đang chờ kết quả gần nhất của một đơn (để bấm lại thì dùng lại, không tạo thêm). */
    Optional<Payment> findFirstByBookingIdAndStatusOrderByIdDesc(Long bookingId, PaymentStatus status);
}
