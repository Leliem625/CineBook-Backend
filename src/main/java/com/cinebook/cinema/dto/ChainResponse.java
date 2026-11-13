package com.cinebook.cinema.dto;

/** Hệ thống rạp kèm số cụm rạp (GET /api/v1/chains). */
public record ChainResponse(String slug, String name, String logoUrl, long cinemaCount) {
}
