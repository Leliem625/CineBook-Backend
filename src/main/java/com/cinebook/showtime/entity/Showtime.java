package com.cinebook.showtime.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import com.cinebook.cinema.entity.ScreenFormat;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

/**
 * Suất chiếu. movieId, roomId, cinemaId là id của module khác (catalog, cinema).
 * cinemaId được phi chuẩn hóa để lọc lịch chiếu theo rạp/khu vực nhanh.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "showtime")
public class Showtime {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long movieId;

    @Column(nullable = false)
    private Long roomId;

    @Column(nullable = false)
    private Long cinemaId;

    @Column(nullable = false)
    private Instant startAt;

    @Column(nullable = false)
    private Instant endAt;

    /** Hết phim + quảng cáo + dọn phòng (BR-50). */
    @Column(nullable = false)
    private Instant occupiedUntil;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ScreenFormat format;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Language language;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ShowtimeStatus status = ShowtimeStatus.SCHEDULED;

    /** Bật phòng chờ ảo (BR-55). */
    @Column(nullable = false)
    private boolean highDemand;

    @Version
    @Column(nullable = false)
    private Integer version;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;
}
