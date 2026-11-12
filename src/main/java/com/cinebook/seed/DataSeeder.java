package com.cinebook.seed;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Điều phối seed dữ liệu khi chạy profile dev. Mỗi seeder tự bỏ qua nếu đã có dữ liệu.
 */
@Component
@Profile("dev")
@RequiredArgsConstructor
public class DataSeeder implements ApplicationRunner {

    private final CinemaSeeder cinemaSeeder;

    @Override
    public void run(ApplicationArguments args) {
        cinemaSeeder.seedIfEmpty();
        // TODO giai đoạn 1: TmdbSyncJob (phim) -> ShowtimeSeeder (suất chiếu 7 ngày tới)
    }
}
