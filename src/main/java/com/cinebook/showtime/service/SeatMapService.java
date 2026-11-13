package com.cinebook.showtime.service;

import com.cinebook.showtime.dto.SeatMapResponse;

/**
 * Snapshot sơ đồ ghế + mức còn ghế (FR-204).
 */
public interface SeatMapService {

    /** Sơ đồ ghế của suất chiếu kèm trạng thái và giá từng ghế; suất không tồn tại -> 404. */
    SeatMapResponse getSeatMap(Long showtimeId);
}
