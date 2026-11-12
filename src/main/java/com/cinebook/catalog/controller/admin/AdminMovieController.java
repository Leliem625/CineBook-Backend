package com.cinebook.catalog.controller.admin;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Quản trị phim (FR-902).
 */
@RestController
@RequestMapping("/api/v1/admin/movies")
public class AdminMovieController {

    // TODO GET / , GET /{id}, PUT /{id}
    // TODO POST /import-tmdb {tmdbId}
    // TODO POST /sync-tmdb  chạy đồng bộ thủ công
}
