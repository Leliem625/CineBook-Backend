package com.cinebook.payment.service;

import com.cinebook.payment.dto.CreatePaymentRequest;
import com.cinebook.payment.dto.PaymentResponse;

/**
 * Tạo thanh toán, xử lý IPN.
 */
public interface PaymentService {

    /**
     * Bắt đầu thanh toán một đơn đang giữ ghế: chuyển đơn sang PAYMENT_PENDING, tạo bản ghi payment và trả
     * URL trang thanh toán của cổng. Bấm lại khi đã có payment PENDING thì trả URL của payment đó.
     * <p>
     * Lỗi: đơn không có hoặc của phiên khác 404; hết hạn giữ ghế 410 {@code HOLD_EXPIRED};
     * cổng chưa cấu hình 503 {@code PAYMENT_UNAVAILABLE}. Mọi lỗi đều rollback, đơn vẫn HELD.
     *
     * @param sessionId id phiên khách lấy từ cookie
     * @param ipAddress IP của khách gửi cho cổng
     */
    PaymentResponse createPayment(String sessionId, Long bookingId, CreatePaymentRequest request, String ipAddress);

    // TODO handleIpn(): kiểm tra chữ ký + idempotent + số tiền (BR-21..23)
}
