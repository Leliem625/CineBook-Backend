package com.cinebook.catalog.service;

import com.cinebook.catalog.dto.ProvinceDto;

import java.util.List;
import java.util.Optional;

/**
 * Danh mục 34 tỉnh/thành (từ 01/07/2025).
 * Module khác (cinema...) lấy thông tin tỉnh qua interface này, không dùng ProvinceRepository trực tiếp.
 */
public interface ProvinceService {

    /** Toàn bộ tỉnh/thành, theo thứ tự hiển thị (sort_order, tên). */
    List<ProvinceDto> getProvince();

    /** Tìm tỉnh theo slug, ví dụ "ha-noi". */
    Optional<ProvinceDto> findBySlug(String slug);
}
