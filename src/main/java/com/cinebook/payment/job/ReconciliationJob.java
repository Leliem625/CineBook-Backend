package com.cinebook.payment.job;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.cinebook.payment.service.ReconciliationService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Định kỳ đóng các payment PENDING quá hạn và nhả ghế của đơn tương ứng (FR-505).
 * Chu kỳ đặt bằng {@code cinebook.payment.reconcile-interval} (mặc định 60s).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReconciliationJob {

    private static final int BATCH_SIZE = 100;

    private final ReconciliationService reconciliationService;

    @Scheduled(fixedDelayString = "${cinebook.payment.reconcile-interval:60s}")
    public void run() {
        int total = 0;
        int processed;
        do {
            processed = reconciliationService.expireStalePayments(BATCH_SIZE);
            total += processed;
        } while (processed == BATCH_SIZE);
        if (total > 0) {
            log.info("ReconciliationJob: đã đóng {} payment quá hạn", total);
        }
    }
}
