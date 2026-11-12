package com.cinebook.cinema.repository;

import com.cinebook.cinema.entity.Cinema;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CinemaRepository extends JpaRepository<Cinema, Long> {
}
