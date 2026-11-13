package com.cinebook.cinema.dto;

/** Cụm rạp (GET /api/v1/cinemas, /cinemas/{id}). Khớp kiểu Cinema ở frontend. */
public record CinemaResponse(
        Long id,
        String name,
        String chain,
        String chainSlug,
        String provinceSlug,
        String provinceName,
        String address,
        String phone,
        Double lat,
        Double lng) {
}
