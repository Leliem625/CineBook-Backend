package com.cinebook.showtime.dto;

import com.cinebook.catalog.dto.MovieResponse;
import com.cinebook.cinema.dto.CinemaResponse;
import com.cinebook.cinema.entity.ScreenFormat;
import com.cinebook.showtime.entity.Language;
import com.cinebook.showtime.entity.ShowtimeStatus;

import java.time.OffsetDateTime;
import java.util.List;

/** Sơ đồ ghế của một suất chiếu (GET /api/v1/showtimes/{id}/seats): thông tin suất + phim + rạp + ghế. */
public record SeatMapResponse(
        Long showtimeId,
        OffsetDateTime startAt,
        ScreenFormat format,
        Language language,
        ShowtimeStatus status,
        MovieResponse movie,
        CinemaResponse cinema,
        String roomName,
        int rows,
        int cols,
        List<SeatView> seats) {
}
