package com.cinebook.showtime.service.impl;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.cinebook.catalog.dto.MovieResponse;
import com.cinebook.catalog.service.MovieService;
import com.cinebook.cinema.dto.CinemaResponse;
import com.cinebook.cinema.entity.ScreenFormat;
import com.cinebook.cinema.service.CinemaService;
import com.cinebook.showtime.dto.MovieShowtimesResponse;
import com.cinebook.showtime.dto.ShowtimeResponse;
import com.cinebook.showtime.entity.Showtime;
import com.cinebook.showtime.entity.ShowtimeStatus;
import com.cinebook.showtime.mapper.ShowtimeMapper;
import com.cinebook.showtime.repository.ShowtimeRepository;
import com.cinebook.showtime.service.ShowtimeService;

/**
 * Cài đặt {@link ShowtimeService}.
 * Rạp của tỉnh lấy qua {@link CinemaService}, thông tin phim lấy qua {@link MovieService};
 * không đọc bảng của module khác trực tiếp.
 */
@Service
// @RequiredArgsConstructor
@Transactional(readOnly = true)
public class ShowtimeServiceImpl implements ShowtimeService {

    private static final ZoneId VN = ZoneId.of("Asia/Ho_Chi_Minh");

    private final ShowtimeRepository showtimeRepository;
    private final CinemaService cinemaService;
    private final MovieService movieService;

    public ShowtimeServiceImpl(ShowtimeRepository showtimeRepository, CinemaService cinemaService,
                               MovieService movieService) {
        this.showtimeRepository = showtimeRepository;
        this.cinemaService = cinemaService;
        this.movieService = movieService;
    }

    @Override
    public List<MovieShowtimesResponse> getShowtimes(String provinceSlug, Long cinemaId, LocalDate date,
                                               Long movieId, ScreenFormat format) {
        boolean hasProvince = provinceSlug != null && !provinceSlug.isBlank();
        if (!hasProvince && cinemaId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cần truyền province hoặc cinema");
        }

        Set<Long> cinemaIds;
        if (cinemaId != null) {
            CinemaResponse cinema = cinemaService.getCinema(cinemaId); // 404 nếu không có
            if (hasProvince && !provinceSlug.equals(cinema.provinceSlug())) {
                return List.of(); // rạp không thuộc tỉnh này
            }
            cinemaIds = Set.of(cinemaId);
        } else {
            cinemaIds = cinemaService.getCinemas(provinceSlug, null).stream() // 404 nếu sai slug
                    .map(CinemaResponse::id)
                    .collect(Collectors.toSet());
        }
        if (cinemaIds.isEmpty()) {
            return List.of();
        }

        // "Ngày" theo giờ Việt Nam: [00:00 hôm đó, 00:00 hôm sau)
        var from = date.atStartOfDay(VN).toInstant();
        var to = date.plusDays(1).atStartOfDay(VN).toInstant();

        // Đã sắp theo giờ bắt đầu; gom theo phim (LinkedHashMap giữ thứ tự: phim chiếu sớm nhất xếp trước)
        Map<Long, List<ShowtimeResponse>> byMovie = showtimeRepository
                .findByCinemaIdInAndStartAtGreaterThanEqualAndStartAtLessThanAndStatusNotOrderByStartAt(
                        cinemaIds, from, to, ShowtimeStatus.CANCELLED)
                .stream()
                .filter(s -> movieId == null || movieId.equals(s.getMovieId()))
                .filter(s -> format == null || format == s.getFormat())
                .collect(Collectors.groupingBy(Showtime::getMovieId, LinkedHashMap::new,
                        Collectors.mapping(s -> ShowtimeMapper.toResponse(s, fromPrice(s.getFormat(), date)),
                                Collectors.toList())));

        Map<Long, MovieResponse> movies = movieService.getMoviesByIds(byMovie.keySet()).stream()
                .collect(Collectors.toMap(MovieResponse::id, Function.identity()));
        return byMovie.entrySet().stream()
                .filter(e -> movies.containsKey(e.getKey()))
                .map(e -> new MovieShowtimesResponse(movies.get(e.getKey()), e.getValue()))
                .toList();
    }

    /**
     * Giá từ (VND) tạm thời: giá cơ bản theo định dạng, cuối tuần +10%, làm tròn lên 1.000đ (BR-11, BR-13).
     * TODO thay bằng PricingService khi có bảng giá theo loại ghế, ngày lễ, khung giờ.
     */
    private static long fromPrice(ScreenFormat format, LocalDate date) {
        long base = switch (format) {
            case F2D -> 65_000;
            case F3D -> 85_000;
            case IMAX -> 120_000;
            case F4DX -> 130_000;
        };
        boolean weekend = date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY;
        long price = weekend ? base * 110 / 100 : base;
        return (price + 999) / 1000 * 1000;
    }
}
