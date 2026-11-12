package com.cinebook.pricing.repository;

import com.cinebook.pricing.entity.Holiday;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HolidayRepository extends JpaRepository<Holiday, Long> {
}
