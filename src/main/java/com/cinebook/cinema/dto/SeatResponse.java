package com.cinebook.cinema.dto;

import com.cinebook.cinema.entity.SeatType;

/** Ghế vật lý trong sơ đồ phòng. (x, y) là vị trí trên lưới, khoảng trống giữa các x là lối đi. */
public record SeatResponse(
        Long id,
        String row,
        int number,
        int x,
        int y,
        SeatType type,
        Long pairSeatId) {
}
