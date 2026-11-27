package com.cinebook.ticket.service.impl;

import java.security.SecureRandom;
import java.util.HexFormat;

import org.springframework.stereotype.Service;

import com.cinebook.ticket.service.QrTokenService;

/** Cài đặt {@link QrTokenService}. */
@Service
public class QrTokenServiceImpl implements QrTokenService {

    private final SecureRandom random = new SecureRandom();

    @Override
    public String newToken() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }
}
