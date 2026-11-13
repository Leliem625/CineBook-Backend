package com.cinebook.cinema.service;

import com.cinebook.cinema.dto.CinemaResponse;
import com.cinebook.cinema.dto.ProvinceSummaryResponse;

import java.util.List;

/**
 * Cụm rạp (FR-201, FR-202).
 */
public interface CinemaService {

    /** 34 tỉnh/thành kèm số cụm rạp đang hoạt động; tỉnh nhiều rạp xếp trước. */
    List<ProvinceSummaryResponse> getProvinceSummaries();

    /**
     * Danh sách cụm rạp đang hoạt động.
     *
     * @param provinceSlug lọc theo tỉnh (null = cả nước); slug không tồn tại -> 404
     * @param chainSlug    lọc theo hệ thống rạp (null = mọi hãng)
     */
    List<CinemaResponse> getCinemas(String provinceSlug, String chainSlug);

    /** Chi tiết 1 cụm rạp; không tồn tại hoặc đã ngừng hoạt động -> 404. */
    CinemaResponse getCinema(Long id);

    // TODO GET /{id}/showtimes?date= (lịch chiếu theo rạp)
}
