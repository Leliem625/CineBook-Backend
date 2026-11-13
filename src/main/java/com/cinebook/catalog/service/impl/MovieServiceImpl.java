package com.cinebook.catalog.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

import com.cinebook.catalog.dto.MovieResponse;
import com.cinebook.catalog.entity.Genre;
import com.cinebook.catalog.entity.Movie;
import com.cinebook.catalog.entity.MovieStatus;
import com.cinebook.catalog.mapper.MovieMapper;
import com.cinebook.catalog.repository.GenreRepository;
import com.cinebook.catalog.repository.MovieRepository;
import com.cinebook.catalog.service.MovieService;
import com.cinebook.catalog.tmdb.TmdbClient;
import com.cinebook.catalog.tmdb.dto.TmdbDtos.MovieSummary;
import com.cinebook.common.util.SlugUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Cài đặt {@link MovieService}.
 * <p>
 * Danh sách phim: gọi TMDB trước (dữ liệu mới nhất), lỗi thì lấy từ DB (fallback).
 * Kết quả TMDB được ghép thêm dữ liệu nội bộ theo tmdbId: id (để nối với suất chiếu),
 * slug, thời lượng, nhãn tuổi, trailer - những thứ API danh sách của TMDB không có.
 * <p>
 * Cố ý KHÔNG đặt @Transactional ở method public: không giữ kết nối DB trong lúc chờ HTTP.
 * Các truy vấn repository tự mở transaction riêng (ngắn).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MovieServiceImpl implements MovieService {

    private static final String IMAGE_BASE = "https://image.tmdb.org/t/p/w500";

    private final TmdbClient tmdbClient;
    private final MovieRepository movieRepository;
    private final GenreRepository genreRepository;

    @Override
    public List<MovieResponse> getMovies(MovieStatus status) {
        if (!tmdbClient.isConfigured()) {
            log.warn("Chưa cấu hình TMDB_TOKEN -> lấy phim từ DB");
            return fromDatabase(status);
        }
        try {
            return fromTmdb(status);
        } catch (RestClientException e) {
            // Timeout, mất mạng, TMDB lỗi 4xx/5xx, bị chặn... -> dùng dữ liệu đã lưu
            log.warn("Gọi TMDB thất bại ({}) -> lấy phim từ DB", e.getMessage());
            return fromDatabase(status);
        }
    }

    // ------------------------------------------------------------------ nguồn chính: TMDB
    private List<MovieResponse> fromTmdb(MovieStatus status) {
        // Giữ thứ tự TMDB trả về; phim xuất hiện ở cả 2 danh sách thì lấy lần đầu
        Map<Long, MovieStatus> statusById = new LinkedHashMap<>();
        Map<Long, MovieSummary> summaries = new LinkedHashMap<>();
        if (status == null || status == MovieStatus.NOW_SHOWING) {
            tmdbClient.nowPlaying().forEach(m -> add(m, MovieStatus.NOW_SHOWING, summaries, statusById));
        }
        if (status == null || status == MovieStatus.COMING_SOON) {
            tmdbClient.upcoming().forEach(m -> add(m, MovieStatus.COMING_SOON, summaries, statusById));
        }

        // Ghép dữ liệu nội bộ: 1 query cho cả danh sách (không N+1)
        List<Integer> tmdbIds = summaries.keySet().stream().map(Long::intValue).toList();
        Map<Integer, Movie> local = movieRepository.findByTmdbIdIn(tmdbIds).stream()
                .collect(Collectors.toMap(Movie::getTmdbId, Function.identity()));
        Map<Integer, String> genreNames = genreRepository.findAll().stream()
                .filter(g -> g.getTmdbId() != null)
                .collect(Collectors.toMap(Genre::getTmdbId, Genre::getName));

        List<MovieResponse> result = new ArrayList<>();
        for (MovieSummary s : summaries.values()) {
            result.add(toResponse(s, statusById.get(s.id()), local.get((int) s.id()), genreNames));
        }
        return result;
    }

    private static void add(MovieSummary m, MovieStatus st, Map<Long, MovieSummary> summaries,
                            Map<Long, MovieStatus> statusById) {
        if (summaries.putIfAbsent(m.id(), m) == null) {
            statusById.put(m.id(), st);
        }
    }

    private static MovieResponse toResponse(MovieSummary s, MovieStatus status, Movie local,
                                            Map<Integer, String> genreNames) {
        List<String> genres = s.genreIds() == null ? List.of() : s.genreIds().stream()
                .map(genreNames::get).filter(Objects::nonNull).sorted(Comparator.naturalOrder()).toList();
        LocalDate release = local != null && local.getReleaseDateVn() != null
                ? local.getReleaseDateVn()
                : parseDate(s.releaseDate());
        return new MovieResponse(
                local == null ? null : local.getId(),                 // null = phim chưa có suất chiếu ở CineBook
                (int) s.id(),
                local != null ? local.getSlug() : SlugUtil.slugify(s.title()) + "-" + s.id(),
                s.title(),
                s.originalTitle(),
                s.posterPath() == null ? null : IMAGE_BASE + s.posterPath(),
                release,
                local == null ? null : local.getAgeRating(),
                local == null ? null : local.getRuntimeMin(),
                BigDecimal.valueOf(s.voteAverage()).setScale(1, RoundingMode.HALF_UP).doubleValue(),
                local == null ? null : local.getTrailerKey(),
                status,
                local != null ? local.getCountry() != null && local.getCountry().contains("VN")
                        : "vi".equals(s.originalLanguage()),
                genres);
    }

    private static LocalDate parseDate(String s) {
        return s == null || s.isBlank() ? null : LocalDate.parse(s);
    }

    // ------------------------------------------------------------------ dự phòng: DB
    private List<MovieResponse> fromDatabase(MovieStatus status) {
        Set<MovieStatus> statuses = status == null
                ? Set.of(MovieStatus.NOW_SHOWING, MovieStatus.COMING_SOON)
                : Set.of(status);
        return movieRepository.findByStatusInOrderByTmdbPopularityDesc(statuses).stream()
                .map(MovieMapper::toResponse)
                .toList();
    }
}
