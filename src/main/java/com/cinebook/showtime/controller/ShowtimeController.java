package com.cinebook.showtime.controller;

import com.cinebook.cinema.entity.ScreenFormat;
import com.cinebook.showtime.dto.MovieShowtimesResponse;
import com.cinebook.showtime.service.ShowtimeService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * Lịch chiếu và sơ đồ ghế (M02, FR-302).
 */
@RestController
@RequestMapping("/api/v1/showtimes")
@RequiredArgsConstructor
public class ShowtimeController {

    private final ShowtimeService showtimeService;

    /**
     * Phim và suất chiếu theo tỉnh / rạp / ngày (FR-203), gom theo phim. Phải có {@code province} hoặc {@code cinema}.
     * <pre>
     * GET /api/v1/showtimes?province=ha-noi&amp;date=2026-11-14
     * GET /api/v1/showtimes?cinema=12&amp;date=2026-11-14
     * GET /api/v1/showtimes?province=ha-noi&amp;date=2026-11-14&amp;movie=3&amp;format=IMAX
     * </pre>
     */
    @GetMapping
    public List<MovieShowtimesResponse> getShowtimes(
            @RequestParam(required = false) String province,
            @RequestParam(required = false) Long cinema,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) Long movie,
            @RequestParam(required = false) ScreenFormat format) {
        return showtimeService.getShowtimes(province, cinema, date, movie, format);
    }

    // TODO GET /{id}/seats  snapshot sơ đồ ghế
}
