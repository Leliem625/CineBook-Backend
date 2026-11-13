package com.cinebook.cinema.service.impl;

import com.cinebook.cinema.dto.RoomLayoutResponse;
import com.cinebook.cinema.dto.SeatResponse;
import com.cinebook.cinema.entity.Room;
import com.cinebook.cinema.repository.RoomRepository;
import com.cinebook.cinema.repository.SeatRepository;
import com.cinebook.cinema.service.RoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Cài đặt {@link RoomService}. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoomServiceImpl implements RoomService {

    private final RoomRepository roomRepository;
    private final SeatRepository seatRepository;

    @Override
    public RoomLayoutResponse getRoomLayout(Long roomId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không có phòng id=" + roomId));
        var seats = seatRepository.findSellableByRoomId(roomId).stream()
                .map(s -> new SeatResponse(
                        s.getId(),
                        s.getRowLabel(),
                        s.getSeatNumber(),
                        s.getGridX(),
                        s.getGridY(),
                        s.getSeatType(),
                        s.getPairSeat() == null ? null : s.getPairSeat().getId()))
                .toList();
        return new RoomLayoutResponse(room.getId(), room.getName(), room.getGridRows(), room.getGridCols(), seats);
    }
}
