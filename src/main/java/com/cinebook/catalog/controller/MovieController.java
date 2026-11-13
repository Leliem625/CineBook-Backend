package com.cinebook.catalog.controller;

import com.cinebook.catalog.dto.MovieResponse;
import com.cinebook.catalog.entity.MovieStatus;
import com.cinebook.catalog.service.MovieService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * API phim công khai (M01).
 */
@RestController
@RequestMapping("/api/v1/movies")
@RequiredArgsConstructor
public class MovieController {

    private final MovieService movieService;

    /**
     * Danh sách phim (FR-101).
     * <pre>
     * GET /api/v1/movies?status=NOW_SHOWING   phim đang chiếu
     * GET /api/v1/movies?status=COMING_SOON   phim sắp chiếu
     * GET /api/v1/movies                      cả hai
     * </pre>
     */
    @GetMapping
    public List<MovieResponse> getMovies(@RequestParam(required = false) MovieStatus status) {
        return movieService.getMovies(status);
    }

    // TODO GET /{slug}      chi tiết phim (FR-103)
    // TODO GET /{id}/showtimes?province=&date=&format=&language=  (FR-104)
}
