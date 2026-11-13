package com.cinebook.booking.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Đơn đặt vé. Tạo ở trạng thái HELD khi khách bấm "Tiếp tục" ở bước chọn ghế.
 * showtimeId, userId, voucherId là id của module khác (showtime, customer, promotion).
 * Tiền là số nguyên VND (BR-13).
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "booking")
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Mã đặt vé 8 ký tự, bỏ ký tự dễ nhầm (0/O, 1/I). */
    @Column(nullable = false, length = 8, unique = true)
    private String code;

    /** Id phiên khách (cookie ký số, FR-301). */
    @Column(nullable = false, length = 64)
    private String guestSessionId;

    /** Tài khoản khách (M10); null khi đặt vé không cần tài khoản. */
    private Long userId;

    @Column(length = 100)
    private String customerName;

    @Column(length = 255)
    private String email;

    /** Chuẩn hóa dạng +84. */
    @Column(length = 20)
    private String phone;

    @Column(nullable = false)
    private Long showtimeId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BookingStatus status = BookingStatus.HELD;

    /** Hết hạn giữ ghế (BR-05); bắt buộc khi status = HELD. */
    private Instant holdExpiresAt;

    @Column(nullable = false)
    private long subtotal;

    @Column(nullable = false)
    private long discount;

    /** Luôn bằng subtotal - discount và không âm (BR-14). */
    @Column(nullable = false)
    private long total;

    private Long voucherId;

    /** BR-10: gửi lại cùng key thì trả kết quả cũ. */
    @Column(length = 100)
    private String idempotencyKey;

    @Version
    @Column(nullable = false)
    private Integer version;

    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BookingSeat> seats = new ArrayList<>();

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;

    /** Thêm ghế vào đơn (giữ hai chiều quan hệ đồng bộ). */
    public void addSeat(BookingSeat seat) {
        seat.setBooking(this);
        seat.setShowtimeId(showtimeId);
        seats.add(seat);
    }
}
