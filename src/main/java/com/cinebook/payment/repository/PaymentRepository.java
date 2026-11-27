package com.cinebook.payment.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import jakarta.persistence.LockModeType;

import com.cinebook.payment.entity.Payment;
import com.cinebook.payment.entity.PaymentStatus;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    /** Lần thanh toán đang chờ kết quả gần nhất của một đơn (để bấm lại thì dùng lại, không tạo thêm). */
    Optional<Payment> findFirstByBookingIdAndStatusOrderByIdDesc(Long bookingId, PaymentStatus status);

    Optional<Payment> findByTxnRef(String txnRef);

    /** Chỉ lấy id, KHÔNG nạp entity: để không có bản cũ trong cache trước khi khóa dòng ở {@link #findByTxnRefForUpdate}. */
    @Query("SELECT p.id FROM Payment p WHERE p.txnRef = :txnRef")
    Optional<Long> findIdByTxnRef(String txnRef);

    /** Khóa dòng payment: hai IPN trùng nhau xử lý lần lượt, lần sau thấy payment đã xử lý rồi. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Payment p WHERE p.txnRef = :txnRef")
    Optional<Payment> findByTxnRefForUpdate(String txnRef);

    /**
     * Khóa và lấy tối đa {@code limit} payment PENDING đã quá {@code timeoutSeconds} giây kể từ lúc tạo, cũ nhất trước
     * (dùng index idx_payment_pending). {@code SKIP LOCKED}: dòng đang được IPN xử lý thì bỏ qua, không chờ.
     * Phải gọi trong transaction để khóa còn hiệu lực tới khi xử lý xong.
     */
    @Query(nativeQuery = true, value = """
            SELECT id FROM payment
             WHERE status = 'PENDING' AND created_at < now() - (:timeoutSeconds * interval '1 second')
             ORDER BY created_at
             LIMIT :limit
             FOR UPDATE SKIP LOCKED
            """)
    List<Long> lockStalePending(long timeoutSeconds, int limit);
}
