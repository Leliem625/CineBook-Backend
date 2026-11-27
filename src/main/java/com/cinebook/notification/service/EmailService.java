package com.cinebook.notification.service;

import com.cinebook.notification.dto.EmailContent;

/**
 * Gửi email qua SMTP (Mailpit khi local, xem thư ở http://localhost:8025).
 */
public interface EmailService {

    /**
     * Gửi một email (HTML kèm bản chữ thường). Lỗi gửi được ghi log và nuốt, KHÔNG ném ra:
     * việc gửi thư không bao giờ được làm hỏng nghiệp vụ gọi nó.
     *
     * @return true nếu SMTP đã nhận thư
     */
    boolean send(String to, EmailContent content);
}
