package com.cinebook.cinema.repository;

import com.cinebook.cinema.entity.Seat;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SeatRepository extends JpaRepository<Seat, Long> {
}
