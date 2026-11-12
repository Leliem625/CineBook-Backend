package com.cinebook.payment.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Callback từ VNPay (FR-501, FR-502).
 */
@RestController
@RequestMapping("/api/v1/payments/vnpay")
public class VnpayController {

    // TODO GET /return  chỉ hiển thị, redirect về frontend (BR-25)
    // TODO GET /ipn     kiểm tra chữ ký + idempotent + số tiền (BR-21..23)
}
