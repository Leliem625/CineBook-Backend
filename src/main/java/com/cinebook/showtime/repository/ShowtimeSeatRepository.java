package com.cinebook.showtime.repository;

import com.cinebook.showtime.entity.ShowtimeSeat;
import com.cinebook.showtime.entity.ShowtimeSeatId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShowtimeSeatRepository extends JpaRepository<ShowtimeSeat, ShowtimeSeatId> {

    // TODO holdSeats: UPDATE showtime_seat SET status='HELD', booking_id=:b
    //      WHERE showtime_id=:s AND seat_id IN (:seats) AND status='AVAILABLE'  -> trả về số dòng (BR-01, BR-03)
}
