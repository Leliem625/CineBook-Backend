package com.cinebook.waitingroom.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Phòng chờ ảo (BR-55, BR-56).
 */
@RestController
@RequestMapping("/api/v1/queue")
public class WaitingRoomController {

    // TODO POST /{showtimeId}/join
    // TODO GET /{showtimeId}/status -> vị trí / access token
}
