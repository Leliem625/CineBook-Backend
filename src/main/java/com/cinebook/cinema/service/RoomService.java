package com.cinebook.cinema.service;

import com.cinebook.cinema.dto.RoomLayoutResponse;

/**
 * Phòng chiếu và sơ đồ ghế vật lý.
 */
public interface RoomService {

    /** Sơ đồ ghế bán được của phòng; phòng không tồn tại -> 404. */
    RoomLayoutResponse getRoomLayout(Long roomId);
}
