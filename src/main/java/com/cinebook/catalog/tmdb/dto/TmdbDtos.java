package com.cinebook.catalog.tmdb.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/** DTO phản hồi của TMDB API v3 (chỉ các trường CineBook dùng). */
public final class TmdbDtos {

    private TmdbDtos() {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Page<T>(int page, @JsonProperty("total_pages") int totalPages, List<T> results) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record MovieSummary(
            long id,
            String title,
            @JsonProperty("original_title") String originalTitle,
            @JsonProperty("original_language") String originalLanguage,
            @JsonProperty("poster_path") String posterPath,
            @JsonProperty("release_date") String releaseDate,
            @JsonProperty("genre_ids") List<Integer> genreIds,
            @JsonProperty("vote_average") double voteAverage,
            double popularity) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record MovieDetail(
            long id,
            String title,
            @JsonProperty("original_title") String originalTitle,
            @JsonProperty("original_language") String originalLanguage,
            @JsonProperty("origin_country") List<String> originCountry,
            String overview,
            Integer runtime,
            @JsonProperty("release_date") String releaseDate,
            @JsonProperty("poster_path") String posterPath,
            @JsonProperty("backdrop_path") String backdropPath,
            @JsonProperty("vote_average") double voteAverage,
            double popularity,
            List<Genre> genres,
            Videos videos,
            Credits credits,
            @JsonProperty("release_dates") ReleaseDates releaseDates) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Genre(int id, String name) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Videos(List<Video> results) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Video(String key, String site, String type, boolean official,
                        @JsonProperty("iso_639_1") String language) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Credits(List<Cast> cast, List<Crew> crew) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Cast(String name, int order) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Crew(String name, String job) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ReleaseDates(List<CountryRelease> results) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CountryRelease(@JsonProperty("iso_3166_1") String country,
                                 @JsonProperty("release_dates") List<Release> releaseDates) {
    }

    /** type: 1 premiere, 2 limited, 3 theatrical, 4 digital, 5 physical, 6 TV. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Release(String certification, @JsonProperty("release_date") String releaseDate, int type) {
    }
}
