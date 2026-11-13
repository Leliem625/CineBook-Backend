package com.cinebook.showtime.dto;

import com.cinebook.catalog.dto.MovieResponse;

import java.util.List;

/** Một phim kèm các suất chiếu của nó trong ngày (phần tử của GET /api/v1/showtimes). */
public record MovieShowtimesResponse(MovieResponse movie, List<ShowtimeResponse> showtimes) {
}
