package com.cinebook.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Bật {@code @Async}: việc phụ như gửi email chạy ngoài luồng xử lý chính (IPN thanh toán không phải chờ SMTP).
 * Dùng executor mặc định của Spring Boot (luồng ảo vì spring.threads.virtual.enabled=true).
 */
@Configuration
@EnableAsync
public class AsyncConfig {
}
