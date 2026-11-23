package com.cinebook.common.exception;

import java.util.Map;

/**
 * Exception nghiệp vụ mang {@link ErrorCode} và dữ liệu chi tiết (ví dụ danh sách ghế bị mất).
 * Kế thừa RuntimeException nên @Transactional tự rollback khi nó được ném ra: đừng bắt rồi nuốt
 * bên trong cùng transaction.
 * <pre>
 * throw new BusinessException(ErrorCode.SEAT_UNAVAILABLE, "Một số ghế không còn trống", Map.of("seatIds", lost));
 * </pre>
 */
public class BusinessException extends RuntimeException {

    private final ErrorCode code;
    private final Map<String, Object> details;

    public BusinessException(ErrorCode code, String message) {
        this(code, message, Map.of());
    }

    public BusinessException(ErrorCode code, String message, Map<String, Object> details) {
        super(message);
        this.code = code;
        this.details = details;
    }

    public ErrorCode getCode() {
        return code;
    }

    /** Dữ liệu thêm đưa vào response (mỗi khóa thành một trường), không chứa thông tin nhạy cảm. */
    public Map<String, Object> getDetails() {
        return details;
    }
}
