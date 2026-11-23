package com.cinebook.common.config;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.cinebook.common.web.GuestSessionFilter;

/**
 * Đăng ký {@link GuestSessionFilter} chỉ cho luồng đặt vé (/api/v1/bookings/**),
 * để các API công khai khác (phim, rạp, lịch chiếu) không bị gắn cookie.
 */
@Configuration
public class GuestSessionConfig {

    @Bean
    FilterRegistrationBean<GuestSessionFilter> guestSessionFilter(
            @Value("${cinebook.guest-session.secret}") String secret,
            @Value("${cinebook.guest-session.cookie-name}") String cookieName,
            @Value("${cinebook.guest-session.max-age-days}") int maxAgeDays) {
        var registration = new FilterRegistrationBean<>(
                new GuestSessionFilter(cookieName, Duration.ofDays(maxAgeDays), secret));
        registration.addUrlPatterns("/api/v1/bookings/*");
        return registration;
    }
}
