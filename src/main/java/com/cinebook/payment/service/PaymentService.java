package com.cinebook.payment.service;

import java.util.Map;

import com.cinebook.payment.dto.CreatePaymentRequest;
import com.cinebook.payment.dto.IpnResponse;
import com.cinebook.payment.dto.PaymentResponse;
import com.cinebook.payment.dto.PaymentResultResponse;

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

    /**
     * Xử lý IPN của cổng (BR-21..23): ghi callback, kiểm tra chữ ký, đối chiếu số tiền, idempotent.
     * Thành công thì payment SUCCEEDED, đơn CONFIRMED, ghế SOLD, sinh vé, tất cả trong một transaction;
     * thất bại thì payment FAILED (hoặc CANCELLED_BY_USER) và đơn quay về HELD để thử lại.
     * Luôn trả phản hồi theo định dạng VNPay; lỗi không lường trước thì ném exception để rollback.
     */
    IpnResponse handleIpn(Map<String, String> params);

    /**
     * Kết quả một lần thanh toán cho trang kết quả, tìm theo mã tham chiếu. Chỉ phiên khách sở hữu đơn mới xem
     * được, ngược lại 404.
     */
    PaymentResultResponse getResult(String sessionId, String txnRef);
}
