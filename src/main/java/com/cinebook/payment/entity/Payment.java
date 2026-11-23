package com.cinebook.payment.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

/**
 * Một lần thanh toán. Mỗi lần khách bấm "Thanh toán" tạo một dòng; một booking có thể thử nhiều lần
 * nhưng chỉ có tối đa một lần SUCCEEDED (ràng buộc ở DB). bookingId là id của module booking.
 * Tiền là số nguyên VND (BR-13).
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "payment")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long bookingId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private PaymentProvider provider;

    /** Mã tham chiếu CineBook tự sinh, gửi cho cổng (vnp_TxnRef). Duy nhất. */
    @Column(nullable = false, length = 64, unique = true)
    private String txnRef;

    /** Mã giao dịch do cổng trả về (vnp_TransactionNo); null tới khi có callback. Duy nhất. */
    @Column(length = 100, unique = true)
    private String providerTxnId;

    @Column(nullable = false)
    private long amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status = PaymentStatus.CREATED;

    /** Mã kết quả cổng trả về (vnp_ResponseCode), để tra cứu khi lỗi. */
    @Column(length = 10)
    private String responseCode;

    /** BR-10: gửi lại cùng key thì trả kết quả cũ. */
    @Column(length = 100)
    private String idempotencyKey;

    @Version
    @Column(nullable = false)
    private Integer version;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;

    /** Thời điểm thanh toán thành công. */
    private Instant paidAt;
}
