package com.cinebook.cinema.service;

import com.cinebook.cinema.entity.Room;
import com.cinebook.cinema.entity.SeatType;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Sinh sơ đồ ghế từ template (FR-903).
 * <p>
 * Quy ước: hàng A gần màn hình nhất; hàng cuối là ghế đôi (BR-06);
 * khối giữa của các hàng phía sau là ghế VIP.
 * <p>
 * Ghi bằng JDBC thay vì JPA vì một phòng có 100-300 ghế: một câu INSERT nhiều dòng
 * nhanh hơn nhiều so với persist từng entity (IDENTITY không batch được).
 */
@Service
@RequiredArgsConstructor
public class SeatLayoutService {

    private final JdbcTemplate jdbc;

    private record SeatRow(long id, String rowLabel, int seatNumber, int gridX, int gridY,
                           SeatType type, Long pairSeatId) {
    }

    /** Sinh ghế cho phòng (phòng phải đã được lưu và có id). Trả về số ghế. */
    @Transactional
    public int generateSeats(Room room, RoomTemplate template) {
        List<SeatRow> layout = buildLayout(template);
        List<Long> ids = jdbc.queryForList(
                "SELECT nextval(pg_get_serial_sequence('seat', 'id')) FROM generate_series(1, ?)",
                Long.class, layout.size());
        // Gán id thật và nối ghế đôi
        List<Object> args = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
                "INSERT INTO seat (id, room_id, row_label, seat_number, grid_x, grid_y, seat_type, pair_seat_id) VALUES ");
        for (int i = 0; i < layout.size(); i++) {
            SeatRow s = layout.get(i);
            Long pairId = s.pairSeatId() == null ? null : ids.get(s.pairSeatId().intValue());
            sql.append(i == 0 ? "" : ",").append("(?,?,?,?,?,?,?,?)");
            args.addAll(List.of(ids.get(i), room.getId(), s.rowLabel(), s.seatNumber(), s.gridX(), s.gridY(),
                    s.type().name()));
            args.add(pairId);
        }
        // Một câu lệnh duy nhất: hai ghế đôi tham chiếu chéo nhau vẫn thỏa khóa ngoại
        jdbc.update(sql.toString(), args.toArray());
        return layout.size();
    }

    /** pairSeatId trong kết quả tạm thời là chỉ số trong danh sách, được đổi sang id thật khi ghi. */
    private List<SeatRow> buildLayout(RoomTemplate t) {
        List<SeatRow> seats = new ArrayList<>();
        int gridCols = t.gridCols();
        int lastRow = t.seatRows() - 1;
        int vipFrom = t.seatRows() / 3;
        boolean hasCenterBlock = t.blocks().size() >= 3;

        for (int y = 0; y < lastRow; y++) {
            String label = rowLabel(y);
            int x = 0;
            int number = 1;
            for (int b = 0; b < t.blocks().size(); b++) {
                boolean centerBlock = hasCenterBlock && b > 0 && b < t.blocks().size() - 1;
                for (int k = 0; k < t.blocks().get(b); k++) {
                    boolean vip = y >= vipFrom && (centerBlock || (!hasCenterBlock && y >= lastRow - 3));
                    seats.add(new SeatRow(0, label, number++, x++, y, vip ? SeatType.VIP : SeatType.STANDARD, null));
                }
                x++; // lối đi
            }
        }
        // Hàng cuối: ghế đôi kéo dài hết chiều ngang, không có lối đi
        String label = rowLabel(lastRow);
        int couples = gridCols / 2;
        int offset = (gridCols - couples * 2) / 2;
        for (int c = 0; c < couples; c++) {
            int left = seats.size();
            seats.add(new SeatRow(0, label, c * 2 + 1, offset + c * 2, lastRow, SeatType.COUPLE, (long) left + 1));
            seats.add(new SeatRow(0, label, c * 2 + 2, offset + c * 2 + 1, lastRow, SeatType.COUPLE, (long) left));
        }
        return seats;
    }

    private static String rowLabel(int index) {
        return String.valueOf((char) ('A' + index));
    }
}
