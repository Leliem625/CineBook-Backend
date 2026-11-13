package com.cinebook.booking.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

/**
 * Lịch sử chuyển trạng thái của booking (máy trạng thái SRS 6.2). Chỉ thêm, không sửa.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "booking_status_history")
public class BookingStatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    /** null khi booking mới tạo. */
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private BookingStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BookingStatus toStatus;

    @Column(length = 200)
    private String reason;

    /** guest / system / admin... */
    @Column(length = 50)
    private String actor;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    public BookingStatusHistory(Booking booking, BookingStatus fromStatus, BookingStatus toStatus,
                                String reason, String actor) {
        this.booking = booking;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.reason = reason;
        this.actor = actor;
    }
}
