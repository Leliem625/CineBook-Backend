package com.cinebook.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Bật {@code @Scheduled} cho các job (hết hạn giữ ghế, đối soát...).
 * TODO thêm ShedLock để job không chạy trùng khi chạy nhiều bản (pod).
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
