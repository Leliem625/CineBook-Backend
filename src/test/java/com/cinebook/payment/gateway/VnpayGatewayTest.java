package com.cinebook.payment.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

class VnpayGatewayTest {

    private static final String PAY_URL = "https://sandbox.example/pay";

    private final VnpayGateway gateway = new VnpayGateway(
            new VnpayProperties("TESTCODE", "TESTSECRET", PAY_URL, "http://localhost:3000/dat-ve/ket-qua"));

    private String url() {
        Instant now = Instant.parse("2026-11-23T05:00:00Z"); // 12:00 giờ VN
        return gateway.createPaymentUrl(new PaymentUrlRequest("TXN123", 150_000, "Thanh toan ve CineBook ABC12345",
                "127.0.0.1", now, now.plusSeconds(600)));
    }

    private static Map<String, String> query(String url) {
        Map<String, String> map = new LinkedHashMap<>();
        for (String pair : URI.create(url).getRawQuery().split("&")) {
            String[] kv = pair.split("=", 2);
            map.put(kv[0], URLDecoder.decode(kv[1], StandardCharsets.UTF_8));
        }
        return map;
    }

    @Test
    void urlHasExpectedFields() {
        String url = url();
        Map<String, String> p = query(url);
        assertThat(url).startsWith(PAY_URL + "?");
        assertThat(p).containsEntry("vnp_Amount", "15000000")           // 150.000đ x 100
                .containsEntry("vnp_TxnRef", "TXN123")
                .containsEntry("vnp_TmnCode", "TESTCODE")
                .containsEntry("vnp_CreateDate", "20261123120000")       // giờ VN
                .containsEntry("vnp_ExpireDate", "20261123121000")
                .containsEntry("vnp_ReturnUrl", "http://localhost:3000/dat-ve/ket-qua");
        assertThat(p.get("vnp_SecureHash")).hasSize(128).matches("[0-9a-f]+"); // SHA-512 = 128 ký tự hex
    }

    @Test
    void ownUrlVerifies() {
        assertThat(gateway.verifyCallback(query(url()))).isTrue();
    }

    @Test
    void tamperedAmountIsRejected() {
        Map<String, String> p = query(url());
        p.put("vnp_Amount", "100");
        assertThat(gateway.verifyCallback(p)).isFalse();
    }

    @Test
    void missingOrWrongSignatureIsRejected() {
        Map<String, String> p = query(url());
        p.put("vnp_SecureHash", "abc");
        assertThat(gateway.verifyCallback(p)).isFalse();
        p.remove("vnp_SecureHash");
        assertThat(gateway.verifyCallback(p)).isFalse();
    }

    @Test
    void differentSecretIsRejected() {
        VnpayGateway other = new VnpayGateway(new VnpayProperties("TESTCODE", "OTHERSECRET", PAY_URL, "x"));
        assertThat(other.verifyCallback(query(url()))).isFalse();
    }
}
