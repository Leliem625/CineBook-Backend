package com.cinebook.showtime.service.impl;

import com.cinebook.showtime.service.ShowtimeSaleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;

/** Cài đặt {@link ShowtimeSaleService}. */
@Slf4j
@Service
@RequiredArgsConstructor
public class ShowtimeSaleServiceImpl implements ShowtimeSaleService {

    private final JdbcTemplate jdbc;

    /** Mở bán mọi suất SCHEDULED bắt đầu trước thời điểm {@code until}. Trả về số ghế đã sinh. */
    @Override
    @Transactional
    public int openSalesUntil(Instant until) {
        int seats = jdbc.update("""
                WITH opened AS (
                    UPDATE showtime SET status = 'ON_SALE', updated_at = now()
                    WHERE status = 'SCHEDULED' AND start_at < ?
                    RETURNING id, room_id
                )
                INSERT INTO showtime_seat (showtime_id, seat_id, status)
                SELECT o.id, s.id, CASE WHEN s.sellable THEN 'AVAILABLE' ELSE 'BLOCKED' END
                FROM opened o JOIN seat s ON s.room_id = o.room_id
                """, Timestamp.from(until));
        log.info("Mở bán các suất chiếu trước {}: sinh {} ghế", until, seats);
        return seats;
    }
}
