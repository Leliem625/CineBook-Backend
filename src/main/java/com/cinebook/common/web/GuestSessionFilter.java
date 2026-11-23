package com.cinebook.common.web;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.Base64;
import java.util.UUID;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Cấp và đọc cookie phiên khách có chữ ký (FR-301).
 * <p>
 * Cookie có dạng {@code <uuid>.<chữ ký HMAC-SHA256>}, httpOnly, SameSite=Lax.
 * Cookie thiếu hoặc sai chữ ký thì coi như khách mới và cấp phiên mới (không báo lỗi).
 * Controller lấy id phiên bằng {@code @RequestAttribute(GuestSessionFilter.ATTRIBUTE)}.
 * <p>
 * Không đánh dấu @Component: filter được đăng ký bằng
 * {@link com.cinebook.common.config.GuestSessionConfig} để chỉ áp dụng cho /api/v1/bookings/**.
 */
public class GuestSessionFilter extends OncePerRequestFilter {

    /** Tên request attribute chứa id phiên khách. */
    public static final String ATTRIBUTE = "guestSessionId";

    private final String cookieName;
    private final Duration maxAge;
    private final SecretKeySpec key;

    public GuestSessionFilter(String cookieName, Duration maxAge, String secret) {
        this.cookieName = cookieName;
        this.maxAge = maxAge;
        this.key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        String id = readValidSessionId(req); // null nếu không có cookie hoặc sai chữ ký
        if (id == null) {
            id = UUID.randomUUID().toString();
            addCookie(res, id, isSecure(req));
        }
        req.setAttribute(ATTRIBUTE, id);
        chain.doFilter(req, res);
    }

    /** Trả id phiên nếu cookie hợp lệ, ngược lại null. */
    private String readValidSessionId(HttpServletRequest req) {
        Cookie[] cookies = req.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (!cookieName.equals(cookie.getName())) {
                continue;
            }
            String value = cookie.getValue();
            int dot = value == null ? -1 : value.lastIndexOf('.');
            if (dot <= 0) {
                return null;
            }
            String id = value.substring(0, dot);
            byte[] given = value.substring(dot + 1).getBytes(StandardCharsets.UTF_8);
            byte[] expected = sign(id).getBytes(StandardCharsets.UTF_8);
            // So sánh hằng thời gian, tránh lộ chữ ký qua thời gian phản hồi
            return MessageDigest.isEqual(given, expected) ? id : null;
        }
        return null;
    }

    private void addCookie(HttpServletResponse res, String id, boolean secure) {
        ResponseCookie cookie = ResponseCookie.from(cookieName, id + "." + sign(id))
                .httpOnly(true)
                .secure(secure) // chỉ khi chạy https; ở local (http) đặt secure thì trình duyệt sẽ bỏ cookie
                .sameSite("Lax")
                .path("/")
                .maxAge(maxAge)
                .build();
        res.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    /** Sau proxy/Ingress, request tới app là http nên phải xem X-Forwarded-Proto. */
    private static boolean isSecure(HttpServletRequest req) {
        return req.isSecure() || "https".equalsIgnoreCase(req.getHeader("X-Forwarded-Proto"));
    }

    private String sign(String id) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256"); // Mac không an toàn khi dùng chung giữa các luồng
            mac.init(key);
            return Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(id.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Không ký được cookie phiên khách", e);
        }
    }
}
