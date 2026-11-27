package com.cinebook.payment.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cinebook.common.web.GuestSessionFilter;
import com.cinebook.payment.dto.PaymentResultResponse;
import com.cinebook.payment.service.PaymentService;

import lombok.RequiredArgsConstructor;

/**
 * Trang kết quả hỏi trạng thái thanh toán theo mã tham chiếu (BR-25: không tin tham số trên URL return).
 * Nằm dưới /bookings/ để có cookie phiên khách; chỉ chủ đơn xem được.
 */
@RestController
@RequestMapping("/api/v1/bookings")
@RequiredArgsConstructor
public class PaymentResultController {

    private final PaymentService paymentService;

    @GetMapping("/payment-result")
    public PaymentResultResponse result(@RequestAttribute(GuestSessionFilter.ATTRIBUTE) String sessionId,
                                        @RequestParam String txnRef) {
        return paymentService.getResult(sessionId, txnRef);
    }
}
