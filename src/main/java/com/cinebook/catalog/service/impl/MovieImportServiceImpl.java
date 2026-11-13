package com.cinebook.catalog.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cinebook.catalog.entity.AgeRating;
import com.cinebook.catalog.entity.Movie;
import com.cinebook.catalog.entity.MovieStatus;
import com.cinebook.catalog.repository.GenreRepository;
import com.cinebook.catalog.repository.MovieRepository;
import com.cinebook.catalog.service.MovieImportService;
import com.cinebook.catalog.tmdb.dto.TmdbDtos;
import com.cinebook.catalog.tmdb.dto.TmdbDtos.MovieDetail;
import com.cinebook.common.util.SlugUtil;

import lombok.RequiredArgsConstructor;

/** Cài đặt {@link MovieImportService}. */
@Service
@RequiredArgsConstructor
public class MovieImportServiceImpl implements MovieImportService {

    private static final ZoneId VN = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final String IMAGE_BASE = "https://image.tmdb.org/t/p/";

    private final MovieRepository movieRepository;
    private final GenreRepository genreRepository;

    @Override
    @Transactional
    public Movie upsert(MovieDetail d) {
        Movie movie = movieRepository.findByTmdbId((int) d.id()).orElseGet(Movie::new);
        if (movie.isManuallyEdited()) {
            return movie; // admin đã sửa tay -> không ghi đè
        }
        movie.setTmdbId((int) d.id());
        movie.setTitleVi(d.title());
        movie.setOriginalTitle(d.originalTitle());
        movie.setOverview(blankToNull(d.overview()));
        movie.setRuntimeMin(d.runtime() == null || d.runtime() <= 0 ? null : d.runtime());
        movie.setReleaseDateVn(releaseDateVn(d));
        movie.setAgeRating(ageRatingVn(d));
        movie.setStatus(movie.getReleaseDateVn() != null && !movie.getReleaseDateVn().isAfter(LocalDate.now(VN))
                ? MovieStatus.NOW_SHOWING : MovieStatus.COMING_SOON);
        movie.setCountry(d.originCountry() == null ? null : String.join(",", d.originCountry()));
        movie.setOriginalLanguage(d.originalLanguage());
        movie.setPosterUrl(d.posterPath() == null ? null : IMAGE_BASE + "w500" + d.posterPath());
        movie.setBackdropUrl(d.backdropPath() == null ? null : IMAGE_BASE + "w1280" + d.backdropPath());
        movie.setTrailerKey(trailerKey(d));
        movie.setDirectors(directors(d));
        movie.setCasts(casts(d));
        movie.setTmdbScore(BigDecimal.valueOf(d.voteAverage()).setScale(1, RoundingMode.HALF_UP));
        movie.setTmdbPopularity(BigDecimal.valueOf(d.popularity()).setScale(3, RoundingMode.HALF_UP));
        movie.setSyncedAt(Instant.now());
        if (movie.getSlug() == null) {
            movie.setSlug(uniqueSlug(d));
        }
        List<Integer> genreIds = d.genres() == null ? List.of() : d.genres().stream().map(TmdbDtos.Genre::id).toList();
        movie.setGenres(new HashSet<>(genreRepository.findByTmdbIdIn(genreIds)));
        return movieRepository.save(movie);
    }

    /** Ngày khởi chiếu tại VN (ưu tiên bản chiếu rạp), không có thì lấy ngày phát hành chung. */
    static LocalDate releaseDateVn(MovieDetail d) {
        return vnReleases(d).stream()
                .filter(r -> r.type() == 3 || r.type() == 2)
                .map(r -> LocalDate.parse(r.releaseDate().substring(0, 10)))
                .min(Comparator.naturalOrder())
                .orElse(d.releaseDate() == null || d.releaseDate().isBlank() ? null : LocalDate.parse(d.releaseDate()));
    }

    private static AgeRating ageRatingVn(MovieDetail d) {
        return vnReleases(d).stream()
                .map(TmdbDtos.Release::certification)
                .filter(Objects::nonNull)
                .map(String::trim)
                .map(c -> {
                    try {
                        return AgeRating.valueOf(c);
                    } catch (IllegalArgumentException e) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null); // TMDB thường chưa có nhãn VN -> admin bổ sung
    }

    private static List<TmdbDtos.Release> vnReleases(MovieDetail d) {
        if (d.releaseDates() == null || d.releaseDates().results() == null) {
            return List.of();
        }
        return d.releaseDates().results().stream()
                .filter(c -> "VN".equals(c.country()))
                .flatMap(c -> c.releaseDates().stream())
                .toList();
    }

    /** Ưu tiên trailer chính thức tiếng Việt, rồi tiếng Anh, rồi teaser. */
    private static String trailerKey(MovieDetail d) {
        if (d.videos() == null || d.videos().results() == null) {
            return null;
        }
        return d.videos().results().stream()
                .filter(v -> "YouTube".equals(v.site()))
                .min(Comparator
                        .comparing((TmdbDtos.Video v) -> !"Trailer".equals(v.type()))
                        .thenComparing(v -> !"vi".equals(v.language()))
                        .thenComparing(v -> !v.official()))
                .map(TmdbDtos.Video::key)
                .orElse(null);
    }

    private static String directors(MovieDetail d) {
        if (d.credits() == null || d.credits().crew() == null) {
            return null;
        }
        return blankToNull(d.credits().crew().stream()
                .filter(c -> "Director".equals(c.job()))
                .map(TmdbDtos.Crew::name).distinct()
                .collect(Collectors.joining(", ")));
    }

    private static String casts(MovieDetail d) {
        if (d.credits() == null || d.credits().cast() == null) {
            return null;
        }
        return blankToNull(d.credits().cast().stream()
                .sorted(Comparator.comparingInt(TmdbDtos.Cast::order))
                .limit(6).map(TmdbDtos.Cast::name)
                .collect(Collectors.joining(", ")));
    }

    private String uniqueSlug(MovieDetail d) {
        String slug = SlugUtil.slugify(d.title());
        return movieRepository.existsBySlug(slug) ? slug + "-" + d.id() : slug;
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s;
    }

    @Override
    public Optional<Movie> findByTmdbId(long tmdbId) {
        return movieRepository.findByTmdbId((int) tmdbId);
    }
}
