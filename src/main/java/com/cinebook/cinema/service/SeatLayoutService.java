package com.cinebook.cinema.service;

import com.cinebook.cinema.entity.Room;

/**
 * Sinh sơ đồ ghế từ template (FR-903).
 * <p>
 * Quy ước: hàng A gần màn hình nhất; hàng cuối là ghế đôi (BR-06);
 * khối giữa của các hàng phía sau là ghế VIP.
 * <p>
 * Ghi bằng JDBC thay vì JPA vì một phòng có 100-300 ghế: một câu INSERT nhiều dòng
 * nhanh hơn nhiều so với persist từng entity (IDENTITY không batch được).
 */
public interface SeatLayoutService {

    /**
     * Sinh toàn bộ ghế cho phòng theo template (phòng phải đã được lưu và có id).
     *
     * @return số ghế đã tạo
     */
    int generateSeats(Room room, RoomTemplate template);
}
