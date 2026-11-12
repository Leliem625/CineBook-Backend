package com.cinebook.catalog.repository;

import com.cinebook.catalog.entity.Movie;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MovieRepository extends JpaRepository<Movie, Long> {

    Optional<Movie> findByTmdbId(Integer tmdbId);

    boolean existsBySlug(String slug);
}
