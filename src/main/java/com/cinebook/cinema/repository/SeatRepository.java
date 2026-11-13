package com.cinebook.cinema.repository;

import com.cinebook.cinema.entity.Seat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface SeatRepository extends JpaRepository<Seat, Long> {

    /** Ghế bán được của phòng, theo thứ tự hàng rồi vị trí ngang. */
    @Query("SELECT s FROM Seat s WHERE s.room.id = :roomId AND s.sellable = true ORDER BY s.gridY, s.gridX")
    List<Seat> findSellableByRoomId(Long roomId);
}
