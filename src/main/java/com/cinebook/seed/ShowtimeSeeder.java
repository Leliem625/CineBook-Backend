package com.cinebook.seed;

import com.cinebook.catalog.entity.Movie;
import com.cinebook.catalog.service.MovieImportService;
import com.cinebook.catalog.tmdb.TmdbClient;
import com.cinebook.catalog.tmdb.dto.TmdbDtos;
import com.cinebook.catalog.tmdb.dto.TmdbDtos.MovieDetail;
import com.cinebook.showtime.repository.ShowtimeRepository;
import com.cinebook.showtime.service.ShowtimeSaleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Sinh suất chiếu 7 ngày tới cho mọi phòng, mô phỏng cách rạp thật xếp lịch:
 * <ul>
 *   <li>Phim lấy từ TMDB (đang chiếu + sắp chiếu tại VN), lưu bản gọn vào bảng movie.</li>
 *   <li>Mỗi rạp chỉ chiếu một nhóm phim ("lineup"): rạp chuỗi lớn 7-11 phim, rạp độc lập 2-4 phim.
 *       Phim càng hot (popularity, điểm) càng nhiều rạp chọn; phim Việt được ưu tiên.</li>
 *   <li>Không chiếu trước ngày khởi chiếu tại VN.</li>
 *   <li>Mỗi phòng xếp kín 9h-24h, nối tiếp nhau, không chồng giờ (BR-50):
 *       chiếm phòng = 15' quảng cáo + thời lượng phim + 15' dọn phòng.</li>
 *   <li>Suất tối (17h-22h) ưu tiên phim hot hơn; phòng IMAX chỉ chiếu bom tấn.</li>
 * </ul>
 * Random seed theo (rạp, ngày) nên chạy lại ra cùng lịch. Chỉ suất trong {@link #SALE_DAYS} ngày đầu
 * được mở bán (sinh ghế), các ngày sau do job mở bán dần.
 */
@Slf4j
@Component
@Profile("dev")
@RequiredArgsConstructor
public class ShowtimeSeeder {

    static final ZoneId VN = ZoneId.of("Asia/Ho_Chi_Minh");
    static final int DAYS = 7;
    static final int SALE_DAYS = 2;
    static final int ADS_MIN = 15;
    static final int CLEANING_MIN = 15;
    static final int DEFAULT_RUNTIME = 120;
    static final LocalTime FIRST_SHOW = LocalTime.of(9, 0);
    static final LocalTime LAST_START = LocalTime.of(23, 0);
    static final Set<String> MAJOR_CHAINS = Set.of("CGV Cinemas", "Lotte Cinema", "Galaxy Cinema", "BHD Star Cineplex",
            "Beta Cinemas", "Cinestar", "Mega GS Cinemas", "Platinum Cineplex", "Starlight");
    /** Thể loại TMDB hay có bản lồng tiếng / 3D: hoạt hình, gia đình, giả tưởng, viễn tưởng. */
    static final Set<Integer> FAMILY_GENRES = Set.of(16, 10751);
    static final Set<Integer> THREE_D_GENRES = Set.of(16, 878, 14, 12, 28);

    private final TmdbClient tmdbClient;
    private final MovieImportService movieImportService;
    private final ShowtimeRepository showtimeRepository;
    private final ShowtimeSaleService saleService;
    private final JdbcTemplate jdbc;
    private final TransactionTemplate tx;

    /** Thông tin phim dùng để xếp lịch. */
    record Film(long movieId, String title, LocalDate release, int runtime, double weight,
                boolean vietnamese, boolean family, boolean threeD, boolean blockbuster) {
    }

    record RoomInfo(long roomId, long cinemaId, String chain, Set<String> formats) {
    }

    /** Không đặt @Transactional ở đây: gọi TMDB (HTTP) không được giữ kết nối DB. */
    public void seedIfEmpty() {
        if (showtimeRepository.count() > 0) {
            log.info("Đã có suất chiếu, bỏ qua ShowtimeSeeder");
            return;
        }
        if (!tmdbClient.isConfigured()) {
            log.warn("Chưa có TMDB_TOKEN trong .env -> bỏ qua ShowtimeSeeder");
            return;
        }
        List<Film> films = importFilms();
        List<RoomInfo> rooms = loadRooms();
        LocalDate today = LocalDate.now(VN);
        Instant now = Instant.now();

        Map<Long, List<RoomInfo>> roomsByCinema = rooms.stream()
                .collect(Collectors.groupingBy(RoomInfo::cinemaId, LinkedHashMap::new, Collectors.toList()));
        List<Object[]> rows = new ArrayList<>();
        for (List<RoomInfo> cinemaRooms : roomsByCinema.values()) {
            RoomInfo any = cinemaRooms.get(0);
            List<Film> lineup = lineup(films, any, today);
            for (int d = 0; d < DAYS; d++) {
                LocalDate date = today.plusDays(d);
                for (RoomInfo room : cinemaRooms) {
                    Random random = new Random(room.roomId() * 31 + date.toEpochDay());
                    scheduleRoomDay(room, date, lineup, random, now, rows);
                }
            }
        }
        // Ghi suất chiếu + mở bán trong 1 transaction: lỗi giữa chừng thì không để lại dữ liệu dở dang
        Integer seats = tx.execute(status -> {
            insertShowtimes(rows);
            return saleService.openSalesUntil(today.plusDays(SALE_DAYS).atStartOfDay(VN).toInstant());
        });
        log.info("ShowtimeSeeder: {} phim, {} phòng, {} suất chiếu ({} ngày), mở bán {} ngày đầu: {} ghế",
                films.size(), rooms.size(), rows.size(), DAYS, SALE_DAYS, seats);
    }

    // ------------------------------------------------------------------ phim
    private List<Film> importFilms() {
        Map<Long, TmdbDtos.MovieSummary> summaries = new LinkedHashMap<>();
        tmdbClient.nowPlaying().forEach(m -> summaries.putIfAbsent(m.id(), m));
        tmdbClient.upcoming().forEach(m -> summaries.putIfAbsent(m.id(), m));

        List<Film> films = new ArrayList<>();
        for (long tmdbId : summaries.keySet()) {
            MovieDetail detail = tmdbClient.movieDetail(tmdbId);
            Movie movie = movieImportService.upsert(detail);
            if (movie.getReleaseDateVn() == null) {
                continue;
            }
            Set<Integer> genres = detail.genres() == null ? Set.of()
                    : detail.genres().stream().map(TmdbDtos.Genre::id).collect(Collectors.toSet());
            boolean vietnamese = detail.originCountry() != null && detail.originCountry().contains("VN");
            int runtime = movie.getRuntimeMin() != null ? movie.getRuntimeMin() : DEFAULT_RUNTIME;
            // Độ hot: popularity (giảm chênh lệch bằng căn bậc hai) x điểm; phim Việt được ưu tiên
            double weight = Math.sqrt(Math.max(detail.popularity(), 1))
                    * (0.6 + Math.max(detail.voteAverage(), 5) / 10)
                    * (vietnamese ? 1.8 : 1.0);
            films.add(new Film(movie.getId(), movie.getTitleVi(), movie.getReleaseDateVn(), runtime, weight,
                    vietnamese, genres.stream().anyMatch(FAMILY_GENRES::contains),
                    genres.stream().anyMatch(THREE_D_GENRES::contains), false));
        }
        // Bom tấn: 4 phim nước ngoài hot nhất, dài từ 90 phút -> được chiếu IMAX
        Set<Long> blockbusters = films.stream()
                .filter(f -> !f.vietnamese() && f.runtime() >= 90)
                .sorted(Comparator.comparingDouble(Film::weight).reversed())
                .limit(4).map(Film::movieId).collect(Collectors.toSet());
        return films.stream().map(f -> new Film(f.movieId(), f.title(), f.release(), f.runtime(), f.weight(),
                f.vietnamese(), f.family(), f.threeD(), blockbusters.contains(f.movieId()))).toList();
    }

    // ------------------------------------------------------------------ rạp chọn phim
    /** Nhóm phim rạp này chiếu trong tuần: chọn ngẫu nhiên có trọng số, không lặp. */
    private List<Film> lineup(List<Film> films, RoomInfo cinema, LocalDate today) {
        Random random = new Random(cinema.cinemaId());
        boolean major = MAJOR_CHAINS.contains(cinema.chain());
        int size = major ? 7 + random.nextInt(5) : 2 + random.nextInt(3);
        List<Film> eligible = films.stream()
                .filter(f -> f.release().isBefore(today.plusDays(DAYS)))
                .sorted(Comparator.comparingDouble(Film::weight).reversed())
                .collect(Collectors.toCollection(ArrayList::new));
        List<Film> chosen = new ArrayList<>();
        if (major) { // rạp chuỗi lớn luôn có 3 phim hot nhất
            eligible.stream().limit(3).forEach(chosen::add);
            eligible.removeAll(chosen);
        }
        while (chosen.size() < size && !eligible.isEmpty()) {
            Film f = weightedPick(eligible, random, 1.0);
            chosen.add(f);
            eligible.remove(f);
        }
        return chosen;
    }

    // ------------------------------------------------------------------ xếp lịch 1 phòng / 1 ngày
    private void scheduleRoomDay(RoomInfo room, LocalDate date, List<Film> lineup, Random random,
                                 Instant now, List<Object[]> rows) {
        boolean imaxRoom = room.formats().contains("IMAX");
        List<Film> showing = lineup.stream()
                .filter(f -> !f.release().isAfter(date))
                .filter(f -> !imaxRoom || f.blockbuster())
                .toList();
        if (showing.isEmpty() && imaxRoom) { // không có bom tấn -> phòng IMAX chiếu 2D như thường
            showing = lineup.stream().filter(f -> !f.release().isAfter(date)).toList();
        }
        if (showing.isEmpty()) {
            return;
        }
        ZonedDateTime cursor = date.atTime(FIRST_SHOW).plusMinutes(5L * random.nextInt(10)).atZone(VN);
        Film previous = null;
        while (!cursor.toLocalTime().isAfter(LAST_START) && cursor.toLocalDate().equals(date)) {
            boolean evening = cursor.getHour() >= 17 && cursor.getHour() < 22;
            Film film = weightedPick(showing, random, evening ? 2.0 : 1.0);
            if (film == previous && showing.size() > 1) { // tránh 2 suất liền nhau cùng phim
                film = weightedPick(showing, random, evening ? 2.0 : 1.0);
            }
            ZonedDateTime end = cursor.plusMinutes(ADS_MIN + film.runtime());
            ZonedDateTime occupiedUntil = end.plusMinutes(CLEANING_MIN);
            if (cursor.toInstant().isAfter(now)) { // không tạo suất đã qua giờ
                rows.add(new Object[]{film.movieId(), room.roomId(), room.cinemaId(),
                        cursor.toOffsetDateTime(), end.toOffsetDateTime(), occupiedUntil.toOffsetDateTime(),
                        format(room, film, imaxRoom, random), language(film, random)});
            }
            previous = film;
            cursor = roundUpTo5(occupiedUntil);
        }
    }

    private static String format(RoomInfo room, Film film, boolean imaxRoom, Random random) {
        if (imaxRoom && film.blockbuster()) {
            return "IMAX";
        }
        if (room.formats().contains("F3D") && film.threeD() && random.nextInt(10) < 3) {
            return "F3D";
        }
        return "F2D";
    }

    private static String language(Film film, Random random) {
        if (film.vietnamese()) {
            return "ORIGINAL";
        }
        return film.family() && random.nextBoolean() ? "DUBBED" : "SUBTITLED";
    }

    /** Chọn ngẫu nhiên theo trọng số; exponent > 1 làm phim hot càng được ưu tiên (giờ vàng). */
    private static Film weightedPick(List<Film> films, Random random, double exponent) {
        double total = films.stream().mapToDouble(f -> Math.pow(f.weight(), exponent)).sum();
        double r = random.nextDouble() * total;
        for (Film f : films) {
            r -= Math.pow(f.weight(), exponent);
            if (r <= 0) {
                return f;
            }
        }
        return films.get(films.size() - 1);
    }

    private static ZonedDateTime roundUpTo5(ZonedDateTime t) {
        int extra = (5 - t.getMinute() % 5) % 5;
        return t.plusMinutes(extra).withSecond(0).withNano(0);
    }

    // ------------------------------------------------------------------ JDBC
    private List<RoomInfo> loadRooms() {
        return jdbc.query("""
                SELECT r.id, r.cinema_id, ch.name AS chain, string_agg(rf.format, ',') AS formats
                FROM room r
                JOIN cinema c ON c.id = r.cinema_id AND c.active
                JOIN cinema_chain ch ON ch.id = c.chain_id
                JOIN room_format rf ON rf.room_id = r.id
                WHERE r.active
                GROUP BY r.id, r.cinema_id, ch.name
                ORDER BY r.cinema_id, r.id
                """, (rs, i) -> new RoomInfo(rs.getLong("id"), rs.getLong("cinema_id"), rs.getString("chain"),
                new HashSet<>(Arrays.asList(rs.getString("formats").split(",")))));
    }

    /** Batch INSERT (reWriteBatchedInserts=true gộp thành INSERT nhiều dòng). Exclusion constraint vẫn kiểm tra BR-50. */
    private void insertShowtimes(List<Object[]> rows) {
        String sql = """
                INSERT INTO showtime (movie_id, room_id, cinema_id, start_at, end_at, occupied_until, format, language, status)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'SCHEDULED')
                """;
        for (int from = 0; from < rows.size(); from += 5_000) {
            jdbc.batchUpdate(sql, rows.subList(from, Math.min(from + 5_000, rows.size())));
        }
    }
}
