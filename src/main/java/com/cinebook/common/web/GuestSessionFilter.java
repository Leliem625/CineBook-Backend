package com.cinebook.common.web;

import org.springframework.stereotype.Component;

/**
 * Cấp và đọc cookie phiên khách có chữ ký (FR-301).
 */
@Component
public class GuestSessionFilter {

    // TODO Nếu chưa có cookie GUEST_SESSION thì tạo UUID, ký HMAC, set httpOnly + SameSite=Lax
}
