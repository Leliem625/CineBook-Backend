package com.cinebook.catalog.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * API phim công khai (M01).
 */
@RestController
@RequestMapping("/api/v1/movies")
public class MovieController {

    // TODO GET /            danh sách phim: status, genre, sort, page (FR-101)
    // TODO GET /{slug}      chi tiết phim (FR-103)
    // TODO GET /{id}/showtimes?province=&date=&format=&language=  (FR-104)
}
