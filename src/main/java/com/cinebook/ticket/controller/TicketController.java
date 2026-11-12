package com.cinebook.ticket.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Tra cứu, xem vé, hủy vé (M04).
 */
@RestController
@RequestMapping("/api/v1")
public class TicketController {

    // TODO POST /lookup {code, emailOrPhone} (FR-401)
    // TODO GET /tickets/{token} (FR-404)
    // TODO POST /bookings/{code}/resend-email (FR-402)
    // TODO POST /bookings/{code}/cancel-otp
    // TODO POST /bookings/{code}/cancel {otp} (BR-30, BR-31)
}
