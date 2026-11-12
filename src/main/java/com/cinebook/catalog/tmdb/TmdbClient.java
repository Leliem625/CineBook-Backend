package com.cinebook.catalog.tmdb;

import org.springframework.stereotype.Component;

/**
 * HTTP client gọi TMDB API (RestClient + Resilience4j retry/timeout/rate limit).
 */
@Component
public class TmdbClient {

    // TODO nowPlaying(region=VN), upcoming(region=VN), movieDetails(id, append=credits,videos)
}
