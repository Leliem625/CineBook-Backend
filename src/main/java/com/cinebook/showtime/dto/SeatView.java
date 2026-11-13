package com.cinebook.showtime.dto;

import com.cinebook.cinema.entity.SeatType;
import com.cinebook.showtime.entity.SeatStatus;

/** Một ghế trong suất chiếu: vị trí, loại, trạng thái và giá (VND). */
public record SeatView(
        Long id,
        String row,
        int number,
        int x,
        int y,
        SeatType type,
        Long pairSeatId,
        SeatStatus status,
        long price) {
}
