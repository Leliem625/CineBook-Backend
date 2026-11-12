package com.cinebook.cinema.service;

import java.util.List;

/**
 * Mẫu phòng chiếu để sinh sơ đồ ghế.
 *
 * @param seatRows số hàng ghế (hàng cuối là ghế đôi)
 * @param blocks   số ghế mỗi khối trong một hàng, giữa các khối là lối đi.
 *                 Ví dụ [3, 8, 3] = 3 ghế | lối đi | 8 ghế | lối đi | 3 ghế
 */
public record RoomTemplate(String code, int seatRows, List<Integer> blocks) {

    public static final RoomTemplate SMALL = new RoomTemplate("SMALL", 8, List.of(5, 5));
    public static final RoomTemplate MEDIUM = new RoomTemplate("MEDIUM", 10, List.of(3, 8, 3));
    public static final RoomTemplate LARGE = new RoomTemplate("LARGE", 12, List.of(4, 10, 4));
    public static final RoomTemplate IMAX = new RoomTemplate("IMAX", 14, List.of(4, 12, 4));

    /** Số cột của lưới sơ đồ (gồm cả lối đi). */
    public int gridCols() {
        return blocks.stream().mapToInt(Integer::intValue).sum() + blocks.size() - 1;
    }
}
