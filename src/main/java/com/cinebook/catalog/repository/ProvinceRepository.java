package com.cinebook.catalog.repository;

import com.cinebook.catalog.entity.Province;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProvinceRepository extends JpaRepository<Province, Long> {

    Optional<Province> findBySlug(String slug);
}
