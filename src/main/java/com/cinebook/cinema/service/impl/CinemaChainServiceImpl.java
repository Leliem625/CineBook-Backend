package com.cinebook.cinema.service.impl;

import com.cinebook.catalog.dto.ProvinceDto;
import com.cinebook.catalog.service.ProvinceService;
import com.cinebook.cinema.dto.ChainResponse;
import com.cinebook.cinema.entity.Cinema;
import com.cinebook.cinema.entity.CinemaChain;
import com.cinebook.cinema.repository.CinemaRepository;
import com.cinebook.cinema.service.CinemaChainService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Cài đặt {@link CinemaChainService}. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CinemaChainServiceImpl implements CinemaChainService {

    private final CinemaRepository cinemaRepository;
    private final ProvinceService provinceService;

    @Override
    public List<ChainResponse> getChains(String provinceSlug) {
        List<Cinema> cinemas;
        if (provinceSlug == null || provinceSlug.isBlank()) {
            cinemas = cinemaRepository.findByActiveTrue();
        } else {
            ProvinceDto province = provinceService.findBySlug(provinceSlug)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "Không có tỉnh/thành: " + provinceSlug));
            cinemas = cinemaRepository.findByActiveTrueAndProvinceId(province.id());
        }
        Map<Long, CinemaChain> chains = cinemas.stream().map(Cinema::getChain)
                .collect(Collectors.toMap(CinemaChain::getId, Function.identity(), (a, b) -> a));
        Map<Long, Long> counts = cinemas.stream()
                .collect(Collectors.groupingBy(c -> c.getChain().getId(), Collectors.counting()));
        return chains.values().stream()
                .map(ch -> new ChainResponse(ch.getSlug(), ch.getName(), ch.getLogoUrl(), counts.get(ch.getId())))
                .sorted(Comparator.comparing((ChainResponse c) -> CinemaServiceImpl.INDEPENDENT.equals(c.name()))
                        .thenComparing(Comparator.comparingLong(ChainResponse::cinemaCount).reversed())
                        .thenComparing(ChainResponse::name))
                .toList();
    }
}
