package com.cinebook.booking.service;

import java.util.Locale;

/** Chuẩn hoá thông tin liên hệ trước khi lưu. Đầu vào đã qua kiểm tra của HoldRequest. */
public final class ContactNormalizer {

    private ContactNormalizer() {
    }

    /** 0912345678 / 84912345678 / +84912345678 đều thành +84912345678. */
    public static String phone(String raw) {
        String digits = raw.trim().replaceFirst("^\\+", "");
        if (digits.startsWith("84")) {
            digits = digits.substring(2);
        } else if (digits.startsWith("0")) {
            digits = digits.substring(1);
        }
        return "+84" + digits;
    }

    /** Email không phân biệt hoa thường nên lưu chữ thường. */
    public static String email(String raw) {
        return raw.trim().toLowerCase(Locale.ROOT);
    }

    /** Bỏ khoảng trắng thừa ở hai đầu và giữa các từ. */
    public static String name(String raw) {
        return raw.trim().replaceAll("\\s+", " ");
    }
}
