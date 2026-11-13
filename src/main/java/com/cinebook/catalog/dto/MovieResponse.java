package com.cinebook.catalog.dto;

import com.cinebook.catalog.entity.AgeRating;
import com.cinebook.catalog.entity.MovieStatus;

import java.time.LocalDate;
import java.util.List;

/**
 * Phim trong danh sách (GET /api/v1/movies).
 * Khớp với kiểu {@code Movie} ở frontend (src/lib/types.ts).
 */
public record MovieResponse(
        Long id,
        Integer tmdbId,
        String slug,
        String title,
        String originalTitle,
        String posterUrl,
        LocalDate releaseDate,
        AgeRating ageRating,
        Integer runtime,
        Double score,
        String trailerKey,
        MovieStatus status,
        boolean vietnamese,
        List<String> genres) {
}
