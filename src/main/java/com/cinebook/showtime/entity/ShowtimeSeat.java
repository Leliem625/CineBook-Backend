package com.cinebook.showtime.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

/**
 * Trạng thái một ghế trong một suất chiếu (BR-01). Bảng nóng nhất hệ thống.
 * Giữ ghế bằng UPDATE có điều kiện status = 'AVAILABLE', không đọc rồi ghi.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "showtime_seat")
public class ShowtimeSeat {

    @EmbeddedId
    private ShowtimeSeatId id;

    @MapsId("showtimeId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "showtime_id")
    private Showtime showtime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private SeatStatus status = SeatStatus.AVAILABLE;

    /** Booking đang giữ hoặc đã mua ghế; null khi AVAILABLE/BLOCKED. */
    private Long bookingId;

    @Version
    @Column(nullable = false)
    private Integer version;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;
}
