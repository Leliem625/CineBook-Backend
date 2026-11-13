package com.cinebook.cinema.service.impl;

import com.cinebook.catalog.dto.ProvinceDto;
import com.cinebook.catalog.service.ProvinceService;
import com.cinebook.cinema.dto.CinemaResponse;
import com.cinebook.cinema.dto.ProvinceSummaryResponse;
import com.cinebook.cinema.entity.Cinema;
import com.cinebook.cinema.mapper.CinemaMapper;
import com.cinebook.cinema.repository.CinemaRepository;
import com.cinebook.cinema.service.CinemaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.text.Collator;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Cài đặt {@link CinemaService}.
 * Thông tin tỉnh lấy qua {@link ProvinceService} (interface của module catalog), không đọc bảng province trực tiếp.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CinemaServiceImpl implements CinemaService {

    static final String INDEPENDENT = "Rạp độc lập";
    private static final Collator VI = Collator.getInstance(Locale.forLanguageTag("vi"));

    private final CinemaRepository cinemaRepository;
    private final ProvinceService provinceService;

    @Override
    public List<ProvinceSummaryResponse> getProvinceSummaries() {
        Map<Long, Long> counts = cinemaRepository.countActiveByProvince().stream()
                .collect(Collectors.toMap(CinemaRepository.ProvinceCount::getProvinceId,
                        CinemaRepository.ProvinceCount::getTotal));
        return provinceService.getProvince().stream()
                .map(p -> new ProvinceSummaryResponse(p.slug(), p.name(), counts.getOrDefault(p.id(), 0L)))
                .sorted(Comparator.comparingLong(ProvinceSummaryResponse::cinemaCount).reversed()
                        .thenComparing(ProvinceSummaryResponse::name, VI))
                .toList();
    }

    @Override
    public List<CinemaResponse> getCinemas(String provinceSlug, String chainSlug) {
        Map<Long, ProvinceDto> provinces = provincesById();
        List<Cinema> cinemas;
        if (provinceSlug == null || provinceSlug.isBlank()) {
            cinemas = cinemaRepository.findByActiveTrue();
        } else {
            ProvinceDto province = provinceService.findBySlug(provinceSlug)
                    .orElseThrow(() -> notFound("Không có tỉnh/thành: " + provinceSlug));
            cinemas = cinemaRepository.findByActiveTrueAndProvinceId(province.id());
        }
        return cinemas.stream()
                .filter(c -> chainSlug == null || chainSlug.isBlank() || chainSlug.equals(c.getChain().getSlug()))
                .sorted(CINEMA_ORDER)
                .map(c -> CinemaMapper.toResponse(c, provinces.get(c.getProvinceId())))
                .toList();
    }

    @Override
    public CinemaResponse getCinema(Long id) {
        Cinema cinema = cinemaRepository.findById(id)
                .filter(Cinema::isActive)
                .orElseThrow(() -> notFound("Không có cụm rạp id=" + id));
        return CinemaMapper.toResponse(cinema, provincesById().get(cinema.getProvinceId()));
    }

    /** Hãng (Rạp độc lập xếp cuối) -> tên rạp, so sánh theo tiếng Việt. */
    static final Comparator<Cinema> CINEMA_ORDER = Comparator
            .comparing((Cinema c) -> INDEPENDENT.equals(c.getChain().getName()))
            .thenComparing(c -> c.getChain().getName(), VI)
            .thenComparing(Cinema::getName, VI);

    private Map<Long, ProvinceDto> provincesById() {
        return provinceService.getProvince().stream().collect(Collectors.toMap(ProvinceDto::id, Function.identity()));
    }

    private static ResponseStatusException notFound(String message) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, message);
    }
}
