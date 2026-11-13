package com.cinebook.cinema.service;

import com.cinebook.cinema.dto.ChainResponse;

import java.util.List;

/**
 * Hệ thống rạp (FR-201).
 */
public interface CinemaChainService {

    /**
     * Các hệ thống rạp kèm số cụm rạp; hãng nhiều rạp xếp trước, "Rạp độc lập" xếp cuối.
     *
     * @param provinceSlug chỉ đếm rạp trong tỉnh này (null = cả nước)
     */
    List<ChainResponse> getChains(String provinceSlug);
}
