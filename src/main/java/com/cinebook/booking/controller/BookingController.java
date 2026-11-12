package com.cinebook.booking.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Luồng đặt vé không cần tài khoản (M03).
 */
@RestController
@RequestMapping("/api/v1/bookings")
public class BookingController {

    // TODO POST /hold  {showtimeId, seatIds} + Idempotency-Key + cookie phiên khách (FR-304)
    // TODO GET /{id}   (chỉ phiên khách sở hữu)
    // TODO PATCH /{id} {combos, voucherCode, customerName, email, phone} (FR-306..308)
    // TODO DELETE /{id}/hold  khách bỏ giữ (FR-312)
    // TODO POST /{id}/payments  tạo thanh toán (FR-310, BR-20)
}
