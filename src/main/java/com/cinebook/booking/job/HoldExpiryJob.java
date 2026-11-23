package com.cinebook.booking.job;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.cinebook.booking.service.HoldExpiryService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Định kỳ quét các đơn HELD quá hạn để nhả ghế (BR-05). Chu kỳ đặt bằng
 * {@code cinebook.booking.expiry-scan-interval} (mặc định 30s): ghế nhả trễ tối đa chừng đó.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HoldExpiryJob {

    private static final int BATCH_SIZE = 200;

    private final HoldExpiryService holdExpiryService;

    @Scheduled(fixedDelayString = "${cinebook.booking.expiry-scan-interval:30s}")
    public void run() {
        int total = 0;
        int processed;
        do {
            processed = holdExpiryService.expireOverdue(BATCH_SIZE);
            total += processed;
        } while (processed == BATCH_SIZE);
        if (total > 0) {
            log.info("HoldExpiryJob: đã hết hạn và nhả ghế của {} đơn", total);
        }
    }
}
