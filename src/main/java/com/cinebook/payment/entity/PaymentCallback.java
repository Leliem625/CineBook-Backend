package com.cinebook.payment.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;

/**
 * Mọi callback nhận từ cổng (IPN và return URL), lưu nguyên văn để đối soát (FR-503).
 * paymentId có thể null: callback sai chữ ký hoặc mã tham chiếu không tồn tại vẫn phải được ghi lại (BR-21).
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "payment_callback")
public class PaymentCallback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Null khi chưa gắn được vào một payment. */
    private Long paymentId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private PaymentProvider provider;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private CallbackType callbackType;

    /** Mã tham chiếu lấy từ callback, để tra cứu khi chưa gắn được payment. */
    @Column(length = 64)
    private String txnRef;

    /** Tham số callback nguyên văn (cột JSONB). */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private Map<String, String> payload;

    @Column(nullable = false)
    private boolean signatureValid;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant receivedAt;
}
