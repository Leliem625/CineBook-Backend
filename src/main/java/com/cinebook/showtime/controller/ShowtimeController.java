package com.cinebook.showtime.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Lịch chiếu và sơ đồ ghế (M02, FR-302).
 */
@RestController
@RequestMapping("/api/v1/showtimes")
public class ShowtimeController {

    // TODO GET /?province=&date=&chain=&cinema=&movie=&format=  (FR-203)
    // TODO GET /{id}/seats  snapshot sơ đồ ghế
}
