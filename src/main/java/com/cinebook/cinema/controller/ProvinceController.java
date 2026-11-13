package com.cinebook.cinema.controller;

import com.cinebook.cinema.dto.ProvinceSummaryResponse;
import com.cinebook.cinema.service.CinemaService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Tỉnh/thành kèm số cụm rạp. Đặt ở module cinema vì "số rạp" là dữ liệu của cinema;
 * module catalog chỉ giữ danh mục tỉnh (tránh phụ thuộc vòng catalog <-> cinema).
 */
@RestController
@RequestMapping("/api/v1/provinces")
@RequiredArgsConstructor
public class ProvinceController {

    private final CinemaService cinemaService;

    /** GET /api/v1/provinces -> [{ slug, name, cinemaCount }], tỉnh nhiều rạp xếp trước. */
    @GetMapping
    public List<ProvinceSummaryResponse> getProvinces() {
        return cinemaService.getProvinceSummaries();
    }
}
