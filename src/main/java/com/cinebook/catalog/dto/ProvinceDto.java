package com.cinebook.catalog.dto;

/** Tỉnh/thành - dữ liệu danh mục module catalog cung cấp cho các module khác. */
public record ProvinceDto(Long id, String slug, String name, int sortOrder) {
}
