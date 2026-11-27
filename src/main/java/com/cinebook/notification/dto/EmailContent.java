package com.cinebook.notification.dto;

/** Nội dung một email: tiêu đề, bản HTML và bản chữ thường (cho ứng dụng thư không hiển thị HTML). */
public record EmailContent(String subject, String html, String text) {
}
