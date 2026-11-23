package com.cinebook.showtime.dto;

import java.time.Instant;

import com.cinebook.cinema.entity.ScreenFormat;
import com.cinebook.showtime.entity.ShowtimeStatus;

/** Thông tin tối thiểu của suất chiếu cho module khác (booking) dùng, không lộ entity. */
public record ShowtimeSummary(
        Long id,
        Long movieId,
        Long cinemaId,
        Long roomId,
        Instant startAt,
        ScreenFormat format,
        ShowtimeStatus status) {
}
