package com.cinebook.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Mã lỗi nghiệp vụ trả về theo RFC 7807. Mỗi mã mang sẵn HTTP status của nó.
 * Frontend dựa vào tên mã (trường {@code code} trong response) nên không đổi tên giữa chừng.
 */
public enum ErrorCode {

    /** Ghế không còn trống (người khác giữ/mua trước). Response kèm {@code seatIds} nếu biết ghế nào. */
    SEAT_UNAVAILABLE(HttpStatus.CONFLICT),
    /** Suất chiếu chưa mở bán, đã đóng bán (BR-08) hoặc đã huỷ. */
    SHOWTIME_NOT_AVAILABLE(HttpStatus.CONFLICT),
    /** Hết thời gian giữ ghế. */
    HOLD_EXPIRED(HttpStatus.GONE),
    BOOKING_NOT_FOUND(HttpStatus.NOT_FOUND),
    VOUCHER_INVALID(HttpStatus.UNPROCESSABLE_ENTITY),
    VOUCHER_EXHAUSTED(HttpStatus.CONFLICT),
    CANCEL_NOT_ALLOWED(HttpStatus.CONFLICT),
    INVALID_OTP(HttpStatus.UNPROCESSABLE_ENTITY),
    QUEUE_TOKEN_REQUIRED(HttpStatus.FORBIDDEN),
    RATE_LIMITED(HttpStatus.TOO_MANY_REQUESTS),
    PAYMENT_SIGNATURE_INVALID(HttpStatus.BAD_REQUEST),
    /** Cổng thanh toán chưa cấu hình hoặc không dùng được. */
    PAYMENT_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE),
    VALIDATION_FAILED(HttpStatus.BAD_REQUEST),
    /** Xung đột dữ liệu chung (vi phạm ràng buộc DB, hai luồng cùng sửa một bản ghi). */
    CONFLICT(HttpStatus.CONFLICT),
    /** Lỗi hệ thống không lường trước; chi tiết chỉ ghi log, không trả cho khách. */
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR);

    private final HttpStatus status;

    ErrorCode(HttpStatus status) {
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
