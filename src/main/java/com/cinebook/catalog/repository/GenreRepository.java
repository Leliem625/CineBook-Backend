package com.cinebook.catalog.repository;

import com.cinebook.catalog.entity.Genre;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface GenreRepository extends JpaRepository<Genre, Long> {

    List<Genre> findByTmdbIdIn(Collection<Integer> tmdbIds);
}
