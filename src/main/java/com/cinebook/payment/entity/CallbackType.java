package com.cinebook.payment.entity;

/** Loại callback từ cổng thanh toán: IPN (cổng gọi server) hoặc RETURN (trình duyệt khách quay về). */
public enum CallbackType {
    IPN, RETURN
}
