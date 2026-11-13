package com.cinebook.showtime.mapper;

import com.cinebook.showtime.dto.ShowtimeResponse;
import com.cinebook.showtime.entity.Showtime;

import java.time.ZoneId;

/** Chuyển entity Showtime -> DTO. Giá do service tính rồi truyền vào. */
public final class ShowtimeMapper {

    private static final ZoneId VN = ZoneId.of("Asia/Ho_Chi_Minh");

    private ShowtimeMapper() {
    }

    public static ShowtimeResponse toResponse(Showtime s, long price) {
        return new ShowtimeResponse(
                s.getId(),
                s.getMovieId(),
                s.getCinemaId(),
                s.getStartAt().atZone(VN).toOffsetDateTime(),
                s.getFormat(),
                s.getLanguage(),
                s.getStatus(),
                price);
    }
}
