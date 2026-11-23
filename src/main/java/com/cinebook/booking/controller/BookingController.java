package com.cinebook.booking.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.cinebook.booking.dto.BookingResponse;
import com.cinebook.booking.dto.HoldRequest;
import com.cinebook.booking.dto.HoldResponse;
import com.cinebook.booking.service.BookingService;
import com.cinebook.booking.service.SeatHoldService;
import com.cinebook.common.web.GuestSessionFilter;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Luồng đặt vé không cần tài khoản (M03). Phiên khách do {@link GuestSessionFilter} cấp qua cookie.
 */
@RestController
@RequestMapping("/api/v1/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final SeatHoldService seatHoldService;
    private final BookingService bookingService;

    /** Khách bấm "Tiếp tục" ở bước chọn ghế: giữ ghế và tạo booking HELD (FR-304). */
    @PostMapping("/hold")
    @ResponseStatus(HttpStatus.CREATED)
    public HoldResponse hold(@RequestAttribute(GuestSessionFilter.ATTRIBUTE) String sessionId,
                             @Valid @RequestBody HoldRequest request) {
        return seatHoldService.hold(sessionId, request);
    }

    /** Chi tiết đơn (chỉ phiên khách sở hữu): trang thanh toán dùng để hiện ghế, tổng tiền và đồng hồ. */
    @GetMapping("/{id}")
    public BookingResponse get(@RequestAttribute(GuestSessionFilter.ATTRIBUTE) String sessionId,
                               @PathVariable("id") Long bookingId) {
        return bookingService.getBooking(sessionId, bookingId);
    }

    /** Khách bỏ giữ ghế (FR-312): nhả ghế, đơn chuyển sang CANCELLED. */
    @DeleteMapping("/{id}/hold")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void release(@RequestAttribute(GuestSessionFilter.ATTRIBUTE) String sessionId,
                        @PathVariable("id") Long bookingId) {
        seatHoldService.release(sessionId, bookingId);
    }

    // TODO Idempotency-Key cho POST /hold (BR-10)
    // TODO PATCH /{id} {combos, voucherCode, customerName, email, phone} (FR-306..308)
    // TODO POST /{id}/payments  tạo thanh toán (FR-310, BR-20)
}
