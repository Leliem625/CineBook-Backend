package com.cinebook.catalog.mapper;

import com.cinebook.catalog.dto.MovieResponse;
import com.cinebook.catalog.entity.Genre;
import com.cinebook.catalog.entity.Movie;

import java.util.Comparator;

/** Chuyển entity Movie -> DTO. (Viết tay; khi DTO nhiều lên có thể đổi sang MapStruct.) */
public final class MovieMapper {

    private MovieMapper() {
    }

    public static MovieResponse toResponse(Movie m) {
        return new MovieResponse(
                m.getId(),
                m.getTmdbId(),
                m.getSlug(),
                m.getTitleVi(),
                m.getOriginalTitle(),
                m.getPosterUrl(),
                m.getReleaseDateVn(),
                m.getAgeRating(),
                m.getRuntimeMin(),
                m.getTmdbScore() == null ? null : m.getTmdbScore().doubleValue(),
                m.getTrailerKey(),
                m.getStatus(),
                m.getCountry() != null && m.getCountry().contains("VN"),
                m.getGenres().stream()
                        .map(Genre::getName)
                        .sorted(Comparator.naturalOrder())
                        .toList());
    }
}
