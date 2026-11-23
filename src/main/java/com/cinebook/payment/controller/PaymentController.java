package com.cinebook.payment.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.cinebook.common.web.GuestSessionFilter;
import com.cinebook.payment.dto.CreatePaymentRequest;
import com.cinebook.payment.dto.PaymentResponse;
import com.cinebook.payment.service.PaymentService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Bắt đầu thanh toán một đơn (FR-310). Đường dẫn nằm dưới /api/v1/bookings/ để dùng chung cookie phiên khách.
 */
@RestController
@RequestMapping("/api/v1/bookings/{bookingId}/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PaymentResponse create(@RequestAttribute(GuestSessionFilter.ATTRIBUTE) String sessionId,
                                  @PathVariable Long bookingId,
                                  @Valid @RequestBody CreatePaymentRequest request,
                                  HttpServletRequest http) {
        return paymentService.createPayment(sessionId, bookingId, request, clientIp(http));
    }

    /** IP khách (sau proxy thì lấy X-Forwarded-For). VNPay chỉ nhận IPv4 nên IPv6 (vd localhost ::1) đổi về 127.0.0.1. */
    private static String clientIp(HttpServletRequest http) {
        String forwarded = http.getHeader("X-Forwarded-For");
        String ip = forwarded != null && !forwarded.isBlank() ? forwarded.split(",")[0].trim() : http.getRemoteAddr();
        return ip.contains(":") ? "127.0.0.1" : ip;
    }
}
