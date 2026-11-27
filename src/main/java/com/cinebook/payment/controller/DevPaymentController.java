package com.cinebook.payment.controller;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cinebook.common.exception.BusinessException;
import com.cinebook.common.exception.ErrorCode;
import com.cinebook.payment.dto.IpnResponse;
import com.cinebook.payment.dto.SimulateRequest;
import com.cinebook.payment.dto.SimulateResponse;
import com.cinebook.payment.entity.Payment;
import com.cinebook.payment.gateway.FakeGateway;
import com.cinebook.payment.repository.PaymentRepository;
import com.cinebook.payment.service.PaymentService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * CHỈ DÙNG KHI PHÁT TRIỂN (profile dev + cổng giả lập): trang cổng giả của frontend gọi vào đây để
 * "khách thanh toán xong". Dựng tham số callback ký đúng chuẩn rồi đi qua đúng {@code handleIpn} thật.
 * Ngoài profile dev hoặc khi dùng VNPay thật thì endpoint này không tồn tại.
 */
@Profile("dev")
@ConditionalOnProperty(name = "cinebook.payment.gateway", havingValue = "fake", matchIfMissing = true)
@RestController
@RequestMapping("/api/v1/dev/payments")
@RequiredArgsConstructor
public class DevPaymentController {

    private final FakeGateway gateway;
    private final PaymentRepository paymentRepository;
    private final PaymentService paymentService;

    @PostMapping("/simulate")
    public SimulateResponse simulate(@Valid @RequestBody SimulateRequest request) {
        Payment payment = paymentRepository.findByTxnRef(request.txnRef())
                .orElseThrow(() -> new BusinessException(ErrorCode.BOOKING_NOT_FOUND, "Không tìm thấy giao dịch"));
        FakeGateway.Callback callback = gateway.callback(payment.getTxnRef(), payment.getAmount(), request.result());
        IpnResponse ipn = paymentService.handleIpn(callback.ipnParams());
        return new SimulateResponse(callback.returnUrl(), ipn.rspCode(), ipn.message());
    }
}
