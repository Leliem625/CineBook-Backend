package com.cinebook.payment.gateway;

import java.nio.charset.StandardCharsets;
import java.net.URLEncoder;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

/**
 * VNPay v2.1.0: dựng URL thanh toán và kiểm tra chữ ký callback.
 * <p>
 * Quy tắc ký: lấy các tham số {@code vnp_*} có giá trị, sắp tên tăng dần, nối {@code tên=giá_trị_đã_URL_encode}
 * bằng {@code &}, rồi HMAC-SHA512 với {@code hashSecret}, ra chuỗi hex. Chữ ký không nằm trong chuỗi được ký.
 */
@Component
@RequiredArgsConstructor
public class VnpayGateway implements PaymentGateway {

    private static final ZoneId VN = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter VNP_DATE = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final String HASH_PARAM = "vnp_SecureHash";
    private static final String HASH_TYPE_PARAM = "vnp_SecureHashType";

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

        String query = encode(params); // chuỗi để ký cũng chính là chuỗi query
        return props.payUrl() + "?" + query + "&" + HASH_PARAM + "=" + sign(query);
    }

    @Override
    public boolean verifyCallback(Map<String, String> params) {
        requireConfigured();
        String given = params.get(HASH_PARAM);
        if (given == null || given.isBlank()) {
            return false;
        }
        Map<String, String> signed = new TreeMap<>();
        params.forEach((k, v) -> {
            if (k.startsWith("vnp_") && !k.equals(HASH_PARAM) && !k.equals(HASH_TYPE_PARAM)
                    && v != null && !v.isEmpty()) {
                signed.put(k, v);
            }
        });
        byte[] expected = sign(encode(signed)).getBytes(StandardCharsets.UTF_8);
        byte[] actual = given.toLowerCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(expected, actual); // so sánh hằng thời gian
    }

    /** Tham số đã sắp xếp (TreeMap) -> {@code a=1&b=2}, bỏ tham số rỗng, giá trị URL-encode ASCII như mẫu của VNPay. */
    private static String encode(Map<String, String> sorted) {
        return sorted.entrySet().stream()
                .filter(e -> e.getValue() != null && !e.getValue().isEmpty())
                .map(e -> e.getKey() + "=" + URLEncoder.encode(e.getValue(), StandardCharsets.US_ASCII))
                .collect(Collectors.joining("&"));
    }

    private String sign(String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA512"); // Mac không an toàn khi dùng chung giữa các luồng
            mac.init(new SecretKeySpec(props.hashSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA512"));
            return HexFormat.of().formatHex(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Không ký được dữ liệu VNPay", e);
        }
    }

    private void requireConfigured() {
        if (!props.isConfigured()) {
            throw new IllegalStateException("Chưa cấu hình VNPAY_TMN_CODE / VNPAY_HASH_SECRET trong .env");
        }
    }
}
