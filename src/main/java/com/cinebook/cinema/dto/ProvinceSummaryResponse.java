package com.cinebook.cinema.dto;

/** Tỉnh/thành kèm số cụm rạp đang hoạt động (GET /api/v1/provinces). Khớp kiểu Province ở frontend. */
public record ProvinceSummaryResponse(String slug, String name, long cinemaCount) {
}
