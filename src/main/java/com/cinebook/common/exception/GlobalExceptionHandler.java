package com.cinebook.common.exception;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import lombok.extern.slf4j.Slf4j;

/**
 * Chuyển exception thành {@link ProblemDetail} (RFC 7807): {@code status}, {@code title}, {@code detail}
 * cộng thêm trường {@code code} (tên {@link ErrorCode}) để frontend xử lý theo mã, không phải đọc chữ.
 * <p>
 * Kế thừa {@link ResponseEntityExceptionHandler} nên các lỗi chuẩn của Spring MVC (JSON sai định dạng,
 * thiếu tham số, {@code ResponseStatusException} 404/400 của các module khác...) vẫn giữ nguyên status
 * và chỉ đổi sang cùng định dạng ProblemDetail.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    /** Lỗi nghiệp vụ chủ động ném ra: status lấy từ ErrorCode, chi tiết thêm vào response. */
    @ExceptionHandler(BusinessException.class)
    ProblemDetail handleBusiness(BusinessException e) {
        return problem(e.getCode(), e.getMessage(), e.getDetails());
    }

    /** {@code @Valid} sai: trả danh sách trường lỗi trong {@code errors}. */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException e,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        e.getBindingResult().getFieldErrors().forEach(f -> errors.putIfAbsent(f.getField(), f.getDefaultMessage()));
        ProblemDetail body = problem(ErrorCode.VALIDATION_FAILED, "Dữ liệu gửi lên không hợp lệ", Map.of("errors", errors));
        return ResponseEntity.status(body.getStatus()).body(body);
    }

    /** Hai luồng giữ các tập ghế chồng nhau bị kẹt khóa: khách chỉ cần thử lại. */
    @ExceptionHandler(PessimisticLockingFailureException.class)
    ProblemDetail handleLockFailure(PessimisticLockingFailureException e) {
        log.warn("Xung đột khóa dòng, khách nên thử lại: {}", e.getMessage());
        return problem(ErrorCode.SEAT_UNAVAILABLE, "Có người đang đặt các ghế này, vui lòng thử lại", Map.of());
    }

    /** Vi phạm ràng buộc DB (unique, khóa ngoại...), ví dụ hai yêu cầu giữ ghế chạy song song. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    ProblemDetail handleIntegrity(DataIntegrityViolationException e) {
        log.warn("Vi phạm ràng buộc dữ liệu: {}", e.getMostSpecificCause().getMessage());
        return problem(ErrorCode.CONFLICT, "Dữ liệu vừa được thay đổi bởi yêu cầu khác, vui lòng thử lại", Map.of());
    }

    /** Hai luồng cùng sửa một bản ghi (@Version lệch). */
    @ExceptionHandler(OptimisticLockingFailureException.class)
    ProblemDetail handleOptimistic(OptimisticLockingFailureException e) {
        log.warn("Xung đột phiên bản dữ liệu: {}", e.getMessage());
        return problem(ErrorCode.CONFLICT, "Dữ liệu vừa được thay đổi bởi yêu cầu khác, vui lòng thử lại", Map.of());
    }

    /** Mọi lỗi còn lại: ghi log đầy đủ, chỉ trả thông báo chung (không lộ SQL, stack trace). */
    @ExceptionHandler(Exception.class)
    ProblemDetail handleUnexpected(Exception e) {
        log.error("Lỗi không lường trước", e);
        return problem(ErrorCode.INTERNAL_ERROR, "Lỗi hệ thống, vui lòng thử lại sau", Map.of());
    }

    private static ProblemDetail problem(ErrorCode code, String message, Map<String, ?> details) {
        ProblemDetail p = ProblemDetail.forStatusAndDetail(code.getStatus(), message);
        p.setTitle(code.name());
        p.setProperty("code", code.name());
        details.forEach(p::setProperty);
        return p;
    }
}
