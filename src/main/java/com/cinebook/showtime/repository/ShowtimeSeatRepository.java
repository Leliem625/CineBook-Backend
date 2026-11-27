package com.cinebook.showtime.repository;

import com.cinebook.showtime.entity.SeatStatus;
import com.cinebook.showtime.entity.ShowtimeSeat;
import com.cinebook.showtime.entity.ShowtimeSeatId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;

public interface ShowtimeSeatRepository extends JpaRepository<ShowtimeSeat, ShowtimeSeatId> {

    /** Chỉ cần (seatId, status), không tải entity. */
    interface SeatState {
        Long getSeatId();

        SeatStatus getStatus();
    }

    @Query("SELECT ss.id.seatId AS seatId, ss.status AS status FROM ShowtimeSeat ss WHERE ss.id.showtimeId = :showtimeId")
    List<SeatState> findStatesByShowtimeId(Long showtimeId);

    // ------------------------------------------------------------------ giữ / nhả ghế (BR-01, BR-03)
    //
    // Hai câu UPDATE bên dưới là native query nên đi thẳng vào DB, bỏ qua @Version của JPA:
    // phải tự tăng cột version. Không dùng clearAutomatically để khỏi làm Booking vừa lưu
    // trong cùng transaction bị "detached"; hold() không nạp entity ShowtimeSeat nên cache không bị cũ.
    //
    // Gọi trong @Transactional và SAU KHI đã lưu booking: DB có CHECK "ghế HELD phải có booking_id"
    // và khóa ngoại tới booking.

    /**
     * Giữ ghế cho một booking: chỉ đổi những ghế đang AVAILABLE, nên giữ nguyên tử và không bao giờ
     * giữ trùng. Nếu hai yêu cầu cùng giành một ghế, yêu cầu sau chờ khóa dòng rồi thấy ghế đã HELD
     * và cập nhật 0 dòng.
     * <p>
     * Người gọi phải so sánh số dòng trả về với số ghế yêu cầu; khác nhau thì rollback toàn bộ (BR-03).
     * Nên truyền {@code seatIds} đã sắp xếp tăng dần để các transaction khóa dòng cùng thứ tự,
     * tránh deadlock khi hai yêu cầu giữ các tập ghế chồng nhau.
     *
     * @return số ghế đã giữ được
     */
    @Modifying(flushAutomatically = true)
    @Query(nativeQuery = true, value = """
            UPDATE showtime_seat
               SET status = 'HELD', booking_id = :bookingId, version = version + 1, updated_at = now()
             WHERE showtime_id = :showtimeId AND seat_id IN (:seatIds) AND status = 'AVAILABLE'
            """)
    int holdSeats(Long showtimeId, Collection<Long> seatIds, Long bookingId);

    /**
     * Nhả mọi ghế đang được booking giữ (HELD), dùng khi khách bỏ giữ, giữ ghế mới thay thế
     * hoặc hết hạn. Ghế SOLD không bị đụng tới.
     *
     * @return số ghế đã nhả
     */
    @Modifying(flushAutomatically = true)
    @Query(nativeQuery = true, value = """
            UPDATE showtime_seat
               SET status = 'AVAILABLE', booking_id = NULL, version = version + 1, updated_at = now()
             WHERE booking_id = :bookingId AND status = 'HELD'
            """)
    int releaseSeats(Long bookingId);

    /**
     * Id các ghế đang được booking này giữ. Dùng sau {@link #holdSeats} khi giữ thiếu:
     * ghế bị mất = ghế yêu cầu trừ đi tập này (kể cả ghế không có dòng trong showtime_seat).
     */
    @Query(nativeQuery = true, value = "SELECT seat_id FROM showtime_seat WHERE booking_id = :bookingId AND status = 'HELD'")
    List<Long> findHeldSeatIds(Long bookingId);

    /**
     * Bán các ghế đang được booking giữ (HELD -> SOLD) khi thanh toán thành công. Người gọi phải so số dòng
     * trả về với số ghế của đơn: lệch nghĩa là có ghế không còn được giữ, phải rollback.
     *
     * @return số ghế đã bán
     */
    @Modifying(flushAutomatically = true)
    @Query(nativeQuery = true, value = """
            UPDATE showtime_seat
               SET status = 'SOLD', version = version + 1, updated_at = now()
             WHERE booking_id = :bookingId AND status = 'HELD'
            """)
    int sellSeats(Long bookingId);
}
