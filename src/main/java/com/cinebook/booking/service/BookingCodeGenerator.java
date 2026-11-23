package com.cinebook.booking.service;

import java.security.SecureRandom;

import org.springframework.stereotype.Component;

import com.cinebook.booking.repository.BookingRepository;

import lombok.RequiredArgsConstructor;

/** Sinh mã đặt vé 8 ký tự ngẫu nhiên, bỏ ký tự dễ nhầm (0/O, 1/I/L). */
@Component
@RequiredArgsConstructor
public class BookingCodeGenerator {

    private static final String ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
    private static final int LENGTH = 8;

    private final BookingRepository bookingRepository;
    private final SecureRandom random = new SecureRandom();

    /** Thử lại nếu trùng mã đã có (xác suất cực nhỏ; cột code còn có UNIQUE làm chốt cuối). */
    public String next() {
        String code;
        do {
            StringBuilder sb = new StringBuilder(LENGTH);
            for (int i = 0; i < LENGTH; i++) {
                sb.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
            }
            code = sb.toString();
        } while (bookingRepository.existsByCode(code));
        return code;
    }
}
