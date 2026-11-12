package com.cinebook.common.util;

import java.text.Normalizer;
import java.util.Locale;

/** Tạo slug không dấu cho URL: "Rạp Đống Đa" -> "rap-dong-da". */
public final class SlugUtil {

    private SlugUtil() {
    }

    public static String slugify(String text) {
        String s = text.replace('đ', 'd').replace('Đ', 'D');
        s = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        return s.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
    }
}
