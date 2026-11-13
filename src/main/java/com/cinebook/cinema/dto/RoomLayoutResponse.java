package com.cinebook.cinema.dto;

import java.util.List;

/** Sơ đồ ghế của một phòng chiếu (chỉ gồm ghế bán được). */
public record RoomLayoutResponse(Long id, String name, int rows, int cols, List<SeatResponse> seats) {
}
