package com.cinebook.catalog.tmdb;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Cấu hình TMDB (application.yml: cinebook.tmdb.*).
 * Lưu ý: dùng api.tmdb.org vì api.themoviedb.org bị nhà mạng VN chặn.
 */
@ConfigurationProperties(prefix = "cinebook.tmdb")
public record TmdbProperties(String baseUrl, String token, String region, String language) {
}
