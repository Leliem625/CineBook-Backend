package com.cinebook.payment.gateway;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Ký và kiểm tra chữ ký theo quy ước VNPay v2.1.0: lấy các tham số {@code vnp_*} có giá trị, sắp tên tăng dần,
 * nối {@code tên=giá_trị_đã_URL_encode} bằng {@code &}, rồi HMAC-SHA512 với khóa bí mật ra chuỗi hex.
 * Chữ ký không nằm trong chuỗi được ký. Dùng chung cho cổng thật ({@link VnpayGateway}) và cổng giả lập.
 */
public class VnpaySigner {

    private static final String HASH_PARAM = "vnp_SecureHash";
    private static final String HASH_TYPE_PARAM = "vnp_SecureHashType";

    private final String secret;

    public VnpaySigner(String secret) {
        this.secret = secret;
    }

    /** Query string đã sắp xếp, đã encode và kèm {@code &vnp_SecureHash=...} ở cuối. */
    public String signedQuery(Map<String, String> params) {
        String query = encode(new TreeMap<>(params));
        return query + "&" + HASH_PARAM + "=" + sign(query);
    }

    /** Bản sao của tham số kèm {@code vnp_SecureHash}, dùng khi cần Map (ví dụ giả lập IPN). */
    public Map<String, String> withSignature(Map<String, String> params) {
        Map<String, String> signed = new TreeMap<>(params);
        signed.put(HASH_PARAM, sign(encode(new TreeMap<>(params))));
        return signed;
    }

    /** Đúng chữ ký không. Thiếu chữ ký hoặc sai thì false; so sánh hằng thời gian. */
    public boolean verify(Map<String, String> params) {
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
        return MessageDigest.isEqual(expected, actual);
    }

    /** Tham số đã sắp xếp -> {@code a=1&b=2}, bỏ tham số rỗng, giá trị URL-encode ASCII như mẫu của VNPay. */
    private static String encode(Map<String, String> sorted) {
        return sorted.entrySet().stream()
                .filter(e -> e.getValue() != null && !e.getValue().isEmpty())
                .map(e -> e.getKey() + "=" + URLEncoder.encode(e.getValue(), StandardCharsets.US_ASCII))
                .collect(Collectors.joining("&"));
    }

    private String sign(String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA512"); // Mac không an toàn khi dùng chung giữa các luồng
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA512"));
            return HexFormat.of().formatHex(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Không ký được dữ liệu VNPay", e);
        }
    }
}
