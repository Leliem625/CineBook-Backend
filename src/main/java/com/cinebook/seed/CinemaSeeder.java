package com.cinebook.seed;

import com.cinebook.catalog.entity.Province;
import com.cinebook.catalog.repository.ProvinceRepository;
import com.cinebook.cinema.entity.Cinema;
import com.cinebook.cinema.entity.CinemaChain;
import com.cinebook.cinema.entity.Room;
import com.cinebook.cinema.entity.ScreenFormat;
import com.cinebook.cinema.repository.CinemaChainRepository;
import com.cinebook.cinema.repository.CinemaRepository;
import com.cinebook.cinema.repository.RoomRepository;
import com.cinebook.cinema.service.RoomTemplate;
import com.cinebook.cinema.service.SeatLayoutService;
import com.cinebook.common.util.SlugUtil;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Nạp hệ thống rạp và cụm rạp thật từ snapshot src/main/resources/seed/cinemas-vn.json
 * (tạo bởi tools/places/build_cinemas.py, gộp OpenStreetMap + Overture Maps, gán theo 34 tỉnh/thành),
 * sau đó sinh phòng chiếu và sơ đồ ghế cho từng rạp.
 * <p>
 * Nguồn: © OpenStreetMap contributors (ODbL), Overture Maps (CDLA-Permissive-2.0).
 * Phòng và ghế là dữ liệu sinh ra, dùng Random theo sourceId nên chạy lại luôn ra cùng kết quả.
 * <p>
 * Seeder là công cụ dev nên được phép dùng repository của nhiều module.
 */
@Slf4j
@Component
@Profile("dev")
@RequiredArgsConstructor
public class CinemaSeeder {

    private static final String SNAPSHOT = "seed/cinemas-vn.json";
    private static final Set<String> MAJOR_CHAINS = Set.of("CGV Cinemas", "Lotte Cinema", "Galaxy Cinema",
            "BHD Star Cineplex", "Beta Cinemas", "Cinestar", "Mega GS Cinemas", "Platinum Cineplex", "Starlight");
    private static final Set<String> IMAX_CHAINS = Set.of("CGV Cinemas", "Lotte Cinema", "Galaxy Cinema",
            "BHD Star Cineplex");

    private final JsonMapper jsonMapper;
    private final ProvinceRepository provinceRepository;
    private final CinemaChainRepository chainRepository;
    private final CinemaRepository cinemaRepository;
    private final RoomRepository roomRepository;
    private final SeatLayoutService seatLayoutService;

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Snapshot(List<String> sources, String boundaries, List<SourceCinema> cinemas) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record SourceCinema(String sourceId, String name, String chain, String provinceSlug, String provinceName,
                     BigDecimal lat, BigDecimal lng, String address, String phone) {
    }

    @Transactional
    public void seedIfEmpty() {
        if (cinemaRepository.count() > 0) {
            log.info("Đã có dữ liệu rạp, bỏ qua CinemaSeeder");
            return;
        }
        Snapshot snapshot = readSnapshot();
        Map<String, Long> provinceIds = provinceRepository.findAll().stream()
                .collect(Collectors.toMap(Province::getSlug, Province::getId));
        Map<String, CinemaChain> chains = new HashMap<>();
        Set<String> usedSlugs = new HashSet<>();
        int cinemaCount = 0, roomCount = 0, seatCount = 0;

        for (SourceCinema src : snapshot.cinemas()) {
            Long provinceId = provinceIds.get(src.provinceSlug());
            if (provinceId == null) {
                log.warn("Không tìm thấy tỉnh '{}' cho rạp {}", src.provinceSlug(), src.name());
                continue;
            }
            CinemaChain chain = chains.computeIfAbsent(src.chain(), this::createChain);
            Cinema cinema = createCinema(src, chain, provinceId, usedSlugs);
            cinemaCount++;

            Random random = new Random(src.sourceId().hashCode());
            boolean major = MAJOR_CHAINS.contains(src.chain());
            int rooms = major ? 4 + random.nextInt(4) : 2 + random.nextInt(3);
            for (int i = 1; i <= rooms; i++) {
                boolean imax = IMAX_CHAINS.contains(src.chain()) && rooms >= 6 && i == rooms;
                RoomTemplate template = imax ? RoomTemplate.IMAX : pickTemplate(random, major);
                Room room = createRoom(cinema, i, imax, major, template);
                seatCount += seatLayoutService.generateSeats(room, template);
                roomCount++;
            }
        }
        log.info("CinemaSeeder: {} hệ thống rạp, {} cụm rạp, {} phòng, {} ghế (nguồn: {})",
                chains.size(), cinemaCount, roomCount, seatCount, String.join("; ", snapshot.sources()));
    }

    private Snapshot readSnapshot() {
        try (InputStream in = new ClassPathResource(SNAPSHOT).getInputStream()) {
            return jsonMapper.readValue(in, Snapshot.class);
        } catch (IOException e) {
            throw new UncheckedIOException("Không đọc được " + SNAPSHOT, e);
        }
    }

    private CinemaChain createChain(String name) {
        CinemaChain chain = new CinemaChain();
        chain.setName(name);
        chain.setSlug(SlugUtil.slugify(name));
        return chainRepository.save(chain);
    }

    private Cinema createCinema(SourceCinema src, CinemaChain chain, Long provinceId, Set<String> usedSlugs) {
        String base = SlugUtil.slugify(src.name() + " " + src.provinceName());
        String slug = base;
        for (int n = 2; !usedSlugs.add(slug); n++) {
            slug = base + "-" + n;
        }
        Cinema cinema = new Cinema();
        cinema.setChain(chain);
        cinema.setProvinceId(provinceId);
        cinema.setName(src.name());
        cinema.setSlug(slug);
        cinema.setAddress(src.address());
        cinema.setLatitude(src.lat());
        cinema.setLongitude(src.lng());
        cinema.setPhone(src.phone());
        cinema.setSourceId(src.sourceId());
        return cinemaRepository.save(cinema);
    }

    private Room createRoom(Cinema cinema, int index, boolean imax, boolean major, RoomTemplate template) {
        Set<ScreenFormat> formats = EnumSet.of(ScreenFormat.F2D);
        if (imax) {
            formats.add(ScreenFormat.IMAX);
        }
        if (imax || index == 1 || (major && index == 2)) {
            formats.add(ScreenFormat.F3D);
        }
        Room room = new Room();
        room.setCinema(cinema);
        room.setName(imax ? "IMAX" : "Phòng " + index);
        room.setGridRows(template.seatRows());
        room.setGridCols(template.gridCols());
        room.setFormats(formats);
        return roomRepository.save(room);
    }

    private static RoomTemplate pickTemplate(Random random, boolean major) {
        int r = random.nextInt(10);
        if (!major) {
            return r < 6 ? RoomTemplate.SMALL : RoomTemplate.MEDIUM;
        }
        return r < 2 ? RoomTemplate.SMALL : r < 7 ? RoomTemplate.MEDIUM : RoomTemplate.LARGE;
    }
}
