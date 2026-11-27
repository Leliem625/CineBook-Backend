package com.cinebook.payment.event;

import java.time.Instant;

import com.cinebook.payment.entity.PaymentProvider;

/**
 * Thanh toán thành công và đơn đã được xác nhận (đã commit). Chỉ mang số liệu, không mang entity,
 * để module khác (notification) dùng mà không phụ thuộc vào cấu trúc của payment.
 * Hiện là sự kiện trong tiến trình; khi có outbox/Kafka sẽ phát ra ngoài (booking.confirmed).
 *
 * @param providerTxnId mã giao dịch do cổng trả về (vnp_TransactionNo)
 */
public record PaymentSucceededEvent(Long bookingId, PaymentProvider provider, String txnRef, String providerTxnId,
                                    long amountVnd, Instant paidAt) {
}
