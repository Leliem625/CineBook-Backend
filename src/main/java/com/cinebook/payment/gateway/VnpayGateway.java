package com.cinebook.payment.gateway;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.TreeMap;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

/**
 * VNPay thật (v2.1.0): dựng URL thanh toán và kiểm tra chữ ký callback. Chỉ bật khi
 * {@code cinebook.payment.gateway=vnpay}; mặc định dùng {@link FakeGateway}.
 */
@Component
@ConditionalOnProperty(name = "cinebook.payment.gateway", havingValue = "vnpay")
@RequiredArgsConstructor
public class VnpayGateway implements PaymentGateway {

    private static final ZoneId VN = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter VNP_DATE = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final VnpayProperties props;

    @Override
    public String createPaymentUrl(PaymentUrlRequest r) {
        requireConfigured();
        Map<String, String> params = new TreeMap<>();
        params.put("vnp_Version", "2.1.0");
        params.put("vnp_Command", "pay");
        params.put("vnp_TmnCode", props.tmnCode());
        params.put("vnp_Amount", String.valueOf(r.amountVnd() * 100)); // VNPay quy ước: nhân 100
        params.put("vnp_CurrCode", "VND");
        params.put("vnp_TxnRef", r.txnRef());
        params.put("vnp_OrderInfo", r.orderInfo());
        params.put("vnp_OrderType", "other");
        params.put("vnp_Locale", "vn");
        params.put("vnp_ReturnUrl", props.returnUrl());
        params.put("vnp_IpAddr", r.ipAddress());
        params.put("vnp_CreateDate", VNP_DATE.format(r.createdAt().atZone(VN)));
        params.put("vnp_ExpireDate", VNP_DATE.format(r.expiresAt().atZone(VN)));
        return props.payUrl() + "?" + signer().signedQuery(params);
    }

    @Override
    public boolean verifyCallback(Map<String, String> params) {
        requireConfigured();
        return signer().verify(params);
    }

    private VnpaySigner signer() {
        return new VnpaySigner(props.hashSecret());
    }

    private void requireConfigured() {
        if (!props.isConfigured()) {
            throw new IllegalStateException("Chưa cấu hình VNPAY_TMN_CODE / VNPAY_HASH_SECRET trong .env");
        }
    }
}
