package com.cinebook.payment.controller;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cinebook.payment.dto.IpnResponse;
import com.cinebook.payment.service.PaymentService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Callback từ VNPay (FR-501, FR-502). Do VNPay gọi từ server của họ nên không có cookie phiên khách,
 * và đường dẫn không nằm dưới /bookings/ để không dính filter phiên. Return URL trỏ thẳng về frontend.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/payments/vnpay")
@RequiredArgsConstructor
public class VnpayController {

    private final PaymentService paymentService;

    /** IPN: VNPay báo kết quả. Luôn trả JSON {@code RspCode/Message}; lỗi bất ngờ trả 99 để VNPay gửi lại. */
    @GetMapping("/ipn")
    public IpnResponse ipn(@RequestParam Map<String, String> params) {
        try {
            return paymentService.handleIpn(params);
        } catch (Exception e) {
            log.error("Lỗi xử lý IPN VNPay (txnRef={})", params.get("vnp_TxnRef"), e);
            return IpnResponse.UNKNOWN_ERROR;
        }
    }
}
