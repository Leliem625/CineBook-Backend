package com.cinebook.showtime.controller.admin;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Quản trị suất chiếu (FR-904, FR-905).
 */
@RestController
@RequestMapping("/api/v1/admin/showtimes")
public class AdminShowtimeController {

    // TODO POST /  tạo suất (BR-50, BR-51)
    // TODO POST /bulk  tạo hàng loạt
    // TODO POST /{id}/cancel  hủy suất (BR-33)
    // TODO PUT /{id}/high-demand  bật/tắt phòng chờ
}
