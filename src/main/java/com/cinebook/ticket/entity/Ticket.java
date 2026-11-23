package com.cinebook.ticket.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

/**
 * Vé điện tử: mỗi ghế của một booking đã thanh toán là một vé (FR-311, FR-404).
 * bookingId, showtimeId, seatId là id của module khác (booking, showtime, cinema).
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "ticket")
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long bookingId;

    @Column(nullable = false)
    private Long showtimeId;

    @Column(nullable = false)
    private Long seatId;

    /** Token ký số, không đoán được, dùng trong mã QR và link vé (FR-404). Duy nhất. */
    @Column(nullable = false, length = 64, unique = true)
    private String qrToken;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TicketStatus status = TicketStatus.VALID;

    private Instant checkedInAt;

    /** Id nhân viên soát vé; chưa có bảng staff nên chưa có quan hệ. */
    private Long checkedInBy;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    public Ticket(Long bookingId, Long showtimeId, Long seatId, String qrToken) {
        this.bookingId = bookingId;
        this.showtimeId = showtimeId;
        this.seatId = seatId;
        this.qrToken = qrToken;
    }
}
