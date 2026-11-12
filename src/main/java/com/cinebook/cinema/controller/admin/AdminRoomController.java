package com.cinebook.cinema.controller.admin;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Quản trị phòng và sơ đồ ghế (FR-903).
 */
@RestController
@RequestMapping("/api/v1/admin/rooms")
public class AdminRoomController {

    // TODO CRUD room
    // TODO POST /{id}/generate-seats  sinh ghế từ template
}
