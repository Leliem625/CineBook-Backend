package com.cinebook.notification.service.impl;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import com.cinebook.notification.dto.EmailContent;
import com.cinebook.notification.service.EmailService;

import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;

/** Cài đặt {@link EmailService} bằng JavaMailSender. */
@Slf4j
@Service
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;
    private final String from;

    public EmailServiceImpl(JavaMailSender mailSender,
                            @Value("${cinebook.mail.from:CineBook <no-reply@cinebook.local>}") String from) {
        this.mailSender = mailSender;
        this.from = from;
    }

    @Override
    public boolean send(String to, EmailContent content) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(content.subject());
            helper.setText(content.text(), content.html()); // (chữ thường, HTML)
            mailSender.send(message);
            log.info("Đã gửi email \"{}\"", content.subject()); // không ghi địa chỉ người nhận vào log
            return true;
        } catch (Exception e) {
            log.error("Gửi email \"{}\" thất bại: {}", content.subject(), e.getMessage());
            return false;
        }
    }
}
