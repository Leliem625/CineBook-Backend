package com.cinebook.showtime.dto;

import com.cinebook.cinema.entity.ScreenFormat;
import com.cinebook.showtime.entity.Language;
import com.cinebook.showtime.entity.ShowtimeStatus;

import java.time.OffsetDateTime;

/**
 * Một suất chiếu (GET /api/v1/showtimes). Khớp kiểu Showtime ở frontend (src/lib/types.ts).
 * startAt là giờ Việt Nam (+07:00); price là giá từ (VND).
 */
public record ShowtimeResponse(
        Long id,
        Long movieId,
        Long cinemaId,
        OffsetDateTime startAt,
        ScreenFormat format,
        Language language,
        ShowtimeStatus status,
        long price) {
}
