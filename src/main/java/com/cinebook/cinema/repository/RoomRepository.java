package com.cinebook.cinema.repository;

import com.cinebook.cinema.entity.Room;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomRepository extends JpaRepository<Room, Long> {
}
