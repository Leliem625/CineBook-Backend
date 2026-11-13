package com.cinebook.showtime.service.impl;

import com.cinebook.catalog.service.MovieService;
import com.cinebook.cinema.dto.RoomLayoutResponse;
import com.cinebook.cinema.service.CinemaService;
import com.cinebook.cinema.service.RoomService;
import com.cinebook.showtime.dto.SeatMapResponse;
import com.cinebook.showtime.dto.SeatView;
import com.cinebook.showtime.entity.SeatStatus;
import com.cinebook.showtime.entity.Showtime;
import com.cinebook.showtime.repository.ShowtimeRepository;
import com.cinebook.showtime.repository.ShowtimeSeatRepository;
import com.cinebook.showtime.service.SeatMapService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Cài đặt {@link SeatMapService}.
 * Ghế vật lý lấy từ {@link RoomService}, phim từ {@link MovieService}, rạp từ {@link CinemaService};
 * module này chỉ đọc bảng showtime và showtime_seat.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SeatMapServiceImpl implements SeatMapService {

    private static final ZoneId VN = ZoneId.of("Asia/Ho_Chi_Minh");

    private final ShowtimeRepository showtimeRepository;
    private final ShowtimeSeatRepository showtimeSeatRepository;
    private final RoomService roomService;
    private final CinemaService cinemaService;
    private final MovieService movieService;

    @Override
    public SeatMapResponse getSeatMap(Long showtimeId) {
        Showtime showtime = showtimeRepository.findById(showtimeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không có suất chiếu id=" + showtimeId));

        RoomLayoutResponse room = roomService.getRoomLayout(showtime.getRoomId());
        Map<Long, SeatStatus> states = showtimeSeatRepository.findStatesByShowtimeId(showtimeId).stream()
                .collect(Collectors.toMap(s -> s.getSeatId(), s -> s.getStatus()));
        var date = showtime.getStartAt().atZone(VN).toLocalDate();

        List<SeatView> seats = room.seats().stream()
                // Ghế không có dòng trong showtime_seat coi như đã khoá, không cho chọn
                .map(s -> new SeatView(s.id(), s.row(), s.number(), s.x(), s.y(), s.type(), s.pairSeatId(),
                        states.getOrDefault(s.id(), SeatStatus.BLOCKED),
                        ShowtimePricing.price(showtime.getFormat(), s.type(), date)))
                .toList();

        return new SeatMapResponse(
                showtime.getId(),
                showtime.getStartAt().atZone(VN).toOffsetDateTime(),
                showtime.getFormat(),
                showtime.getLanguage(),
                showtime.getStatus(),
                movieService.getMoviesByIds(List.of(showtime.getMovieId())).stream().findFirst().orElse(null),
                cinemaService.getCinema(showtime.getCinemaId()),
                room.name(),
                room.rows(),
                room.cols(),
                seats);
    }
}
