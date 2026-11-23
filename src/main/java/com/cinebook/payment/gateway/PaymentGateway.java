package com.cinebook.payment.gateway;

import java.util.Map;

/**
 * Giao diện chung cho cổng thanh toán (VNPay, MoMo, Stripe).
 */
public interface PaymentGateway {

    /** URL trang thanh toán của cổng, đã ký. Frontend chuyển trình duyệt của khách tới đó. */
    String createPaymentUrl(PaymentUrlRequest request);

    /** Kiểm tra chữ ký của tham số callback (IPN hoặc return URL). Sai chữ ký thì không được tin dữ liệu. */
    boolean verifyCallback(Map<String, String> params);

    // TODO queryTransaction (đối soát khi mất IPN), refund
}
