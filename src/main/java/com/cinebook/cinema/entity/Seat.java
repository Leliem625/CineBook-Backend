package com.cinebook.cinema.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Ghế vật lý. gridX/gridY là vị trí trên lưới để vẽ sơ đồ. */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "seat")
public class Seat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id")
    private Room room;

    @Column(nullable = false, length = 3)
    private String rowLabel;

    @Column(nullable = false)
    private Integer seatNumber;

    @Column(name = "grid_x", nullable = false)
    private Integer gridX;

    @Column(name = "grid_y", nullable = false)
    private Integer gridY;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private SeatType seatType = SeatType.STANDARD;

    /** Ghế đôi: trỏ tới ghế cặp (BR-06). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pair_seat_id")
    private Seat pairSeat;

    @Column(nullable = false)
    private boolean sellable = true;
}
