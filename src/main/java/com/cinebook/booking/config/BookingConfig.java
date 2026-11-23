package com.cinebook.booking.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** Đăng ký {@link BookingProperties} thành bean để inject vào các service của module booking. */
@Configuration
@EnableConfigurationProperties(BookingProperties.class)
public class BookingConfig {
}
