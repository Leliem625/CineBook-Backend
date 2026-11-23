package com.cinebook.booking.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/** Cấu hình đặt vé, đọc từ cinebook.booking.* trong application.yml. */
@ConfigurationProperties(prefix = "cinebook.booking")
public record BookingProperties(
        /** BR-04: thời gian giữ ghế. */
        @DefaultValue("10m") Duration holdDuration,
        /** BR-06: ngừng bán trước giờ chiếu bấy nhiêu phút. */
        @DefaultValue("15m") Duration salesCutoffBeforeStart,
        /** BR-02: số ghế tối đa mỗi đơn. */
        @DefaultValue("8") int maxSeatsPerBooking) {
}
