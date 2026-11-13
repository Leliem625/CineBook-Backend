package com.cinebook.cinema.controller;

import com.cinebook.cinema.dto.CinemaResponse;
import com.cinebook.cinema.service.CinemaService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Cụm rạp (FR-202).
 */
@RestController
@RequestMapping("/api/v1/cinemas")
@RequiredArgsConstructor
public class CinemaController {

    private final CinemaService cinemaService;

    /**
     * Danh sách cụm rạp.
     * <pre>
     * GET /api/v1/cinemas                                  cả nước
     * GET /api/v1/cinemas?province=ha-noi                  theo tỉnh
     * GET /api/v1/cinemas?province=ha-noi&chain=cgv-cinemas theo tỉnh + hãng
     * </pre>
     */
    @GetMapping
    public List<CinemaResponse> getCinemas(@RequestParam(required = false) String province,
                                           @RequestParam(required = false) String chain) {
        return cinemaService.getCinemas(province, chain);
    }

    /** GET /api/v1/cinemas/{id} -> chi tiết 1 cụm rạp. */
    @GetMapping("/{id}")
    public CinemaResponse getCinema(@PathVariable Long id) {
        return cinemaService.getCinema(id);
    }

    // TODO GET /{id}/showtimes?date=
}
