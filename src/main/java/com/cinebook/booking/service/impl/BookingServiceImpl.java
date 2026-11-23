package com.cinebook.booking.service.impl;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cinebook.booking.dto.BookingResponse;
import com.cinebook.booking.dto.PaymentTarget;
import com.cinebook.booking.entity.Booking;
import com.cinebook.booking.entity.BookingStatus;
import com.cinebook.booking.entity.BookingStatusHistory;
import com.cinebook.booking.repository.BookingRepository;
import com.cinebook.booking.repository.BookingStatusHistoryRepository;
import com.cinebook.booking.service.BookingService;
import com.cinebook.cinema.dto.SeatResponse;
import com.cinebook.cinema.service.RoomService;
import com.cinebook.common.exception.BusinessException;
import com.cinebook.common.exception.ErrorCode;
import com.cinebook.showtime.service.ShowtimeService;

import lombok.RequiredArgsConstructor;

/** Cài đặt {@link BookingService}. Nhãn ghế (hàng, số) lấy qua {@link RoomService}, không đọc bảng cinema. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final BookingStatusHistoryRepository historyRepository;
    private final ShowtimeService showtimeService;
    private final RoomService roomService;

    @Override
    public BookingResponse getBooking(String sessionId, Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .filter(b -> b.getGuestSessionId().equals(sessionId))
                .orElseThrow(() -> new BusinessException(ErrorCode.BOOKING_NOT_FOUND, "Không tìm thấy đơn đặt vé"));

        Long roomId = showtimeService.getShowtimeSummary(booking.getShowtimeId()).roomId();
        Map<Long, SeatResponse> layout = roomService.getRoomLayout(roomId).seats().stream()
                .collect(Collectors.toMap(SeatResponse::id, Function.identity()));

        List<BookingResponse.Seat> seats = booking.getSeats().stream()
                .map(bs -> {
                    SeatResponse s = layout.get(bs.getSeatId());
                    return new BookingResponse.Seat(bs.getSeatId(), s.row(), s.number(), bs.getSeatType(), bs.getPrice());
                })
                .sorted(Comparator.comparing(BookingResponse.Seat::row).thenComparingInt(BookingResponse.Seat::number))
                .toList();

        return new BookingResponse(booking.getId(), booking.getCode(), booking.getStatus(), booking.getShowtimeId(),
                seats, booking.getSubtotal(), booking.getDiscount(), booking.getTotal(),
                booking.getCustomerName(), booking.getEmail(), booking.getPhone(),
                booking.getHoldExpiresAt(), Instant.now());
    }

    @Override
    @Transactional
    public PaymentTarget beginPayment(String sessionId, Long bookingId) {
        // Đổi trạng thái trước (một câu UPDATE có điều kiện), rồi mới nạp entity
        if (bookingRepository.markPaymentPending(bookingId, sessionId) == 1) {
            Booking booking = bookingRepository.findById(bookingId).orElseThrow();
            historyRepository.save(new BookingStatusHistory(booking, BookingStatus.HELD,
                    BookingStatus.PAYMENT_PENDING, "Khách bấm thanh toán", "guest"));
            return toTarget(booking, false);
        }
        // Không đổi được: xem vì sao để trả đúng lỗi
        Booking booking = bookingRepository.findById(bookingId)
                .filter(b -> b.getGuestSessionId().equals(sessionId))
                .orElseThrow(() -> new BusinessException(ErrorCode.BOOKING_NOT_FOUND, "Không tìm thấy đơn đặt vé"));
        return switch (booking.getStatus()) {
            case PAYMENT_PENDING -> {
                // Đang chờ thanh toán nhưng đã quá hạn giữ ghế: không cấp lại URL (cổng sẽ từ chối)
                if (booking.getHoldExpiresAt() == null || !booking.getHoldExpiresAt().isAfter(Instant.now())) {
                    throw new BusinessException(ErrorCode.HOLD_EXPIRED, "Đã hết thời gian giữ ghế");
                }
                yield toTarget(booking, true);
            }
            // HELD mà không chuyển được nghĩa là đã quá hạn giữ ghế (job chưa kịp dọn)
            case HELD, EXPIRED -> throw new BusinessException(ErrorCode.HOLD_EXPIRED, "Đã hết thời gian giữ ghế");
            case CANCELLED -> throw new BusinessException(ErrorCode.CONFLICT, "Đơn đã bị huỷ");
            default -> throw new BusinessException(ErrorCode.CONFLICT, "Đơn không thể thanh toán ở trạng thái hiện tại");
        };
    }

    private static PaymentTarget toTarget(Booking b, boolean alreadyPending) {
        return new PaymentTarget(b.getId(), b.getCode(), b.getTotal(), b.getHoldExpiresAt(), alreadyPending);
    }
}
