package com.cinebook.booking.dto;

import java.util.List;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Yêu cầu giữ ghế kèm thông tin liên hệ của khách (FR-307). Số ghế tối đa (BR-02) kiểm tra trong
 * service theo {@code BookingProperties.maxSeatsPerBooking}, không cố định ở đây.
 *
 * @param showtimeId   suất chiếu muốn đặt
 * @param seatIds      id các ghế muốn giữ (bảng seat); trùng id sẽ bị service loại bỏ
 * @param customerName họ tên người nhận vé
 * @param email        email nhận vé
 * @param phone        số điện thoại di động VN: 0912345678, 84912345678 hoặc +84912345678
 *                     (lưu dạng +84912345678)
 */
public record HoldRequest(
        @NotNull(message = "Thiếu suất chiếu") Long showtimeId,
        @NotEmpty(message = "Chưa chọn ghế") List<@NotNull(message = "Id ghế không hợp lệ") Long> seatIds,
        @NotBlank(message = "Vui lòng nhập họ tên")
        @Size(max = 100, message = "Họ tên tối đa 100 ký tự") String customerName,
        @NotBlank(message = "Vui lòng nhập email")
        @Email(message = "Email không hợp lệ")
        @Size(max = 255, message = "Email tối đa 255 ký tự") String email,
        @NotBlank(message = "Vui lòng nhập số điện thoại")
        @Pattern(regexp = "^(\\+?84|0)[35789]\\d{8}$", message = "Số điện thoại không hợp lệ") String phone) {
}
