package com.cinebook.payment.gateway;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.TreeMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.cinebook.payment.dto.SimulatedResult;

/**
 * Cổng thanh toán GIẢ LẬP VNPay cho môi trường phát triển (mặc định, {@code cinebook.payment.gateway=fake}).
 * URL thanh toán trỏ tới trang giả của frontend; kết quả được giả lập bằng tham số ký đúng chuẩn VNPay nên toàn bộ
 * code thật phía sau (kiểm tra chữ ký, đối chiếu số tiền, xác nhận đơn) vẫn được chạy.
 * Khóa dưới đây chỉ để ký giả lập, KHÔNG phải bí mật thật.
 */
@Component
@ConditionalOnProperty(name = "cinebook.payment.gateway", havingValue = "fake", matchIfMissing = true)
public class FakeGateway implements PaymentGateway {

    private static final VnpaySigner SIGNER = new VnpaySigner("cinebook-dev-fake-secret");
    private static final DateTimeFormatter PAY_DATE = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final ZoneId VN = ZoneId.of("Asia/Ho_Chi_Minh");

    private final VnpayProperties props;
    private final String frontendUrl;
    private final SecureRandom random = new SecureRandom();

    public FakeGateway(VnpayProperties props, @Value("${cinebook.frontend-url}") String frontendUrl) {
        this.props = props;
        this.frontendUrl = frontendUrl;
    }

    /** IPN giả lập và URL return (cùng bộ tham số, đã ký). */
    public record Callback(Map<String, String> ipnParams, String returnUrl) {
    }

    @Override
    public String createPaymentUrl(PaymentUrlRequest r) {
        return frontendUrl + "/gia-lap-vnpay?txnRef=" + enc(r.txnRef()) + "&amount=" + r.amountVnd()
                + "&orderInfo=" + enc(r.orderInfo());
    }

    @Override
    public boolean verifyCallback(Map<String, String> params) {
        return SIGNER.verify(params);
    }

    /** Dựng tham số callback như VNPay sẽ gửi sau khi khách thanh toán xong. */
    public Callback callback(String txnRef, long amountVnd, SimulatedResult result) {
        String code = switch (result) {
            case SUCCESS -> "00";
            case FAILED -> "51";     // tài khoản không đủ số dư
            case CANCELLED -> "24";  // khách huỷ giao dịch
        };
        Map<String, String> params = new TreeMap<>();
        params.put("vnp_TmnCode", "FAKE");
        params.put("vnp_Amount", String.valueOf(amountVnd * 100));
        params.put("vnp_BankCode", "NCB");
        params.put("vnp_OrderInfo", "Thanh toan gia lap");
        params.put("vnp_PayDate", PAY_DATE.format(ZonedDateTime.now(VN)));
        params.put("vnp_ResponseCode", code);
        params.put("vnp_TransactionNo", String.valueOf(10_000_000L + random.nextInt(90_000_000)));
        params.put("vnp_TransactionStatus", code);
        params.put("vnp_TxnRef", txnRef);
        return new Callback(SIGNER.withSignature(params), props.returnUrl() + "?" + SIGNER.signedQuery(params));
    }

    private static String enc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }
}
