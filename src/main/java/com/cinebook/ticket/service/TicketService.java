package com.cinebook.ticket.service;

import java.util.List;
import java.util.Map;

/**
 * Sinh vé khi đơn được xác nhận (FR-311).
 */
public interface TicketService {

    /** Mỗi ghế một vé VALID với token QR riêng. Phải gọi trong transaction xác nhận đơn. */
    void issueTickets(Long bookingId, Long showtimeId, List<Long> seatIds);

    /** Token QR của các vé trong đơn, theo id ghế. Đơn chưa có vé thì rỗng. */
    Map<Long, String> getQrTokens(Long bookingId);
}
