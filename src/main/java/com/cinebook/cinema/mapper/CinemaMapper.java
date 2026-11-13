package com.cinebook.cinema.mapper;

import com.cinebook.catalog.dto.ProvinceDto;
import com.cinebook.cinema.dto.CinemaResponse;
import com.cinebook.cinema.entity.Cinema;

/** Chuyển entity Cinema -> DTO. Thông tin tỉnh truyền vào từ ProvinceService (module catalog). */
public final class CinemaMapper {

    private CinemaMapper() {
    }

    public static CinemaResponse toResponse(Cinema c, ProvinceDto province) {
        return new CinemaResponse(
                c.getId(),
                c.getName(),
                c.getChain().getName(),
                c.getChain().getSlug(),
                province == null ? null : province.slug(),
                province == null ? null : province.name(),
                c.getAddress(),
                c.getPhone(),
                c.getLatitude() == null ? null : c.getLatitude().doubleValue(),
                c.getLongitude() == null ? null : c.getLongitude().doubleValue());
    }
}
