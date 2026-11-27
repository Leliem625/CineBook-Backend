package com.cinebook.payment.dto;

/** Kết quả xử lý IPN giả lập và URL return (kèm tham số đã ký) để trang giả đưa trình duyệt về. */
public record SimulateResponse(String redirectUrl, String rspCode, String message) {
}
