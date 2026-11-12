package com.cinebook.catalog.repository;

import com.cinebook.catalog.entity.Movie;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MovieRepository extends JpaRepository<Movie, Long> {
}
