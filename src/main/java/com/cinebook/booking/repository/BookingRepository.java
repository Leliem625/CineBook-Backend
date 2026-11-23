package com.cinebook.booking.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import com.cinebook.booking.entity.Booking;
import com.cinebook.booking.entity.BookingStatus;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    /** Đơn đang giữ ghế của phiên này ở suất này (DB đảm bảo tối đa 1: uq_booking_one_hold, BR-04). */
    Optional<Booking> findByGuestSessionIdAndShowtimeIdAndStatus(String guestSessionId, Long showtimeId,
                                                                BookingStatus status);

    boolean existsByCode(String code);

    /**
     * Khóa và lấy tối đa {@code limit} đơn HELD đã quá hạn giữ ghế, cũ nhất trước.
     * {@code SKIP LOCKED}: dòng đang bị luồng khác khóa (job khác hoặc khách đang chuyển sang thanh toán)
     * thì bỏ qua, không chờ. Phải gọi trong transaction để khóa còn hiệu lực tới khi xử lý xong.
     */
    @Query(nativeQuery = true, value = """
            SELECT id FROM booking
             WHERE status = 'HELD' AND hold_expires_at < now()
             ORDER BY hold_expires_at
             LIMIT :limit
             FOR UPDATE SKIP LOCKED
            """)
    List<Long> lockOverdueHeld(int limit);

    /**
     * Chuyển HELD -> PAYMENT_PENDING nếu và chỉ nếu đơn thuộc phiên này, đang HELD và CHƯA quá hạn giữ ghế.
     * Một câu UPDATE nên kiểm tra và đổi là nguyên tử: job hết hạn (FOR UPDATE) và việc thanh toán tranh nhau
     * thì chỉ một bên thắng. Native nên bỏ qua @Version, phải tự tăng version; gọi TRƯỚC khi nạp entity
     * Booking trong transaction để không bị entity cũ trong cache.
     *
     * @return 1 nếu đã chuyển, 0 nếu không thoả điều kiện
     */
    @Modifying(flushAutomatically = true)
    @Query(nativeQuery = true, value = """
            UPDATE booking
               SET status = 'PAYMENT_PENDING', version = version + 1, updated_at = now()
             WHERE id = :bookingId AND guest_session_id = :sessionId
               AND status = 'HELD' AND hold_expires_at > now()
            """)
    int markPaymentPending(Long bookingId, String sessionId);
}
