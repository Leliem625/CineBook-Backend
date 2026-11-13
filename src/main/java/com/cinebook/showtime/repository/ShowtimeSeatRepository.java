package com.cinebook.showtime.repository;

import com.cinebook.showtime.entity.SeatStatus;
import com.cinebook.showtime.entity.ShowtimeSeat;
import com.cinebook.showtime.entity.ShowtimeSeatId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ShowtimeSeatRepository extends JpaRepository<ShowtimeSeat, ShowtimeSeatId> {

    /** Chỉ cần (seatId, status), không tải entity. */
    interface SeatState {
        Long getSeatId();

        SeatStatus getStatus();
    }

    @Query("SELECT ss.id.seatId AS seatId, ss.status AS status FROM ShowtimeSeat ss WHERE ss.id.showtimeId = :showtimeId")
    List<SeatState> findStatesByShowtimeId(Long showtimeId);

    // TODO holdSeats: UPDATE showtime_seat SET status='HELD', booking_id=:b
    //      WHERE showtime_id=:s AND seat_id IN (:seats) AND status='AVAILABLE'  -> trả về số dòng (BR-01, BR-03)
}
