package com.cinebook.catalog.tmdb;

import org.springframework.stereotype.Component;

/**
 * Job đồng bộ phim hằng ngày (FR-105).
 */
@Component
public class TmdbSyncJob {

    // TODO @Scheduled(cron) -> upsert theo tmdb_id
}
