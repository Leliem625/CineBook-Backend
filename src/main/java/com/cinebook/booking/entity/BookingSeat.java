package com.cinebook.booking.entity;

import com.cinebook.cinema.entity.SeatType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Ghế trong booking, giá đã chốt tại thời điểm giữ (BR-12).
 * seatId là id của module cinema; showtimeId lặp lại từ booking để DB ràng buộc đúng suất chiếu.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "booking_seat")
public class BookingSeat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    /** Được {@link Booking#addSeat} gán từ booking, không cần set tay. */
    @Column(nullable = false)
    private Long showtimeId;

    @Column(nullable = false)
    private Long seatId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private SeatType seatType;

    /** Giá VND đã chốt. */
    @Column(nullable = false)
    private long price;

    public BookingSeat(Long seatId, SeatType seatType, long price) {
        this.seatId = seatId;
        this.seatType = seatType;
        this.price = price;
    }
}
