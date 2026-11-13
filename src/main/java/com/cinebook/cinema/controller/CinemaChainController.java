package com.cinebook.cinema.controller;

import com.cinebook.cinema.dto.ChainResponse;
import com.cinebook.cinema.service.CinemaChainService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Hệ thống rạp (FR-201).
 */
@RestController
@RequestMapping("/api/v1/chains")
@RequiredArgsConstructor
public class CinemaChainController {

    private final CinemaChainService chainService;

    /** GET /api/v1/chains[?province=ha-noi] -> các hãng kèm số cụm rạp. */
    @GetMapping
    public List<ChainResponse> getChains(@RequestParam(required = false) String province) {
        return chainService.getChains(province);
    }
}
