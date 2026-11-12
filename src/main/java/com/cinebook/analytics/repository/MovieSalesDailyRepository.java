package com.cinebook.analytics.repository;

import com.cinebook.analytics.entity.MovieSalesDaily;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MovieSalesDailyRepository extends JpaRepository<MovieSalesDaily, Long> {
}
