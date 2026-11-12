package com.cinebook.analytics.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Read model thống kê vé bán theo phim/ngày.
 */
@Entity
@Table(name = "movie_sales_daily")
public class MovieSalesDaily {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // TODO fields: movie, date, ticketsSold, revenue
}
