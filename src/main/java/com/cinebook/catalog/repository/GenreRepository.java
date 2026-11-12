package com.cinebook.catalog.repository;

import com.cinebook.catalog.entity.Genre;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GenreRepository extends JpaRepository<Genre, Long> {
}
