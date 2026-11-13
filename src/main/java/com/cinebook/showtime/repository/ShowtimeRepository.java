package com.cinebook.showtime.repository;

import com.cinebook.showtime.entity.Showtime;
import com.cinebook.showtime.entity.ShowtimeStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

public interface ShowtimeRepository extends JpaRepository<Showtime, Long> {

    /** Suất chiếu của các rạp trong khoảng [from, to), không lấy suất ở trạng thái {@code excluded}. */
    List<Showtime> findByCinemaIdInAndStartAtGreaterThanEqualAndStartAtLessThanAndStatusNotOrderByStartAt(
            Collection<Long> cinemaIds, Instant from, Instant to, ShowtimeStatus excluded);
}
