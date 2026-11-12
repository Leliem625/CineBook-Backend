package com.cinebook.showtime.service;

import java.time.Instant;

/**
 * Mở bán suất chiếu: SCHEDULED -> ON_SALE và sinh trạng thái từng ghế (showtime_seat).
 * <p>
 * Vì sao không sinh ghế ngay khi tạo suất chiếu: 1.600 phòng x ~6 suất/ngày x 7 ngày x ~150 ghế
 * ≈ 10 triệu dòng, phần lớn chưa ai xem tới. Giống rạp thật, chỉ mở bán vài ngày trước giờ chiếu,
 * nên bảng nóng showtime_seat chỉ chứa các suất đang bán -> nhỏ hơn nhiều.
 * <p>
 * Toàn bộ là một câu SQL set-based (UPDATE ... RETURNING + INSERT ... SELECT), không kéo dữ liệu về Java.
 */
public interface ShowtimeSaleService {

    /**
     * Mở bán mọi suất SCHEDULED bắt đầu trước thời điểm {@code until}:
     * chuyển sang ON_SALE và sinh trạng thái từng ghế (showtime_seat).
     *
     * @return số ghế đã sinh
     */
    int openSalesUntil(Instant until);
}
