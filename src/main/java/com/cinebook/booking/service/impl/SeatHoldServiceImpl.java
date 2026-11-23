package com.cinebook.booking.service.impl;

import java.time.Instant;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cinebook.booking.config.BookingProperties;
import com.cinebook.booking.dto.HoldRequest;
import com.cinebook.booking.dto.HoldResponse;
import com.cinebook.booking.entity.Booking;
import com.cinebook.booking.entity.BookingSeat;
import com.cinebook.booking.entity.BookingStatus;
import com.cinebook.booking.entity.BookingStatusHistory;
import com.cinebook.booking.repository.BookingRepository;
import com.cinebook.booking.repository.BookingStatusHistoryRepository;
import com.cinebook.booking.service.BookingCodeGenerator;
import com.cinebook.booking.service.ContactNormalizer;
import com.cinebook.booking.service.SeatHoldService;
import com.cinebook.cinema.dto.SeatResponse;
import com.cinebook.cinema.entity.SeatType;
import com.cinebook.cinema.service.RoomService;
import com.cinebook.common.exception.BusinessException;
import com.cinebook.common.exception.ErrorCode;
import com.cinebook.pricing.service.PricingService;
import com.cinebook.showtime.dto.ShowtimeSummary;
import com.cinebook.showtime.entity.ShowtimeStatus;
import com.cinebook.showtime.repository.ShowtimeSeatRepository;
import com.cinebook.showtime.service.ShowtimeService;

import lombok.RequiredArgsConstructor;

/**
 * Cài đặt {@link SeatHoldService}.
 * <p>
 * Thứ tự trong một transaction: kiểm tra đầu vào (không đụng DB ghi) -> nhả hold cũ của phiên ->
 * lưu booking (có booking.id) -> UPDATE ghế có điều kiện AVAILABLE -> so số dòng. Lệch thì ném
 * BusinessException, transaction rollback nên booking vừa lưu biến mất (BR-03).
 */
@Service
@RequiredArgsConstructor
public class SeatHoldServiceImpl implements SeatHoldService {

    private static final ZoneId VN = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final String ACTOR = "guest";

    private final BookingRepository bookingRepository;
    private final BookingStatusHistoryRepository historyRepository;
    private final ShowtimeSeatRepository showtimeSeatRepository;
    private final ShowtimeService showtimeService;
    private final RoomService roomService;
    private final PricingService pricingService;
    private final BookingCodeGenerator codeGenerator;
    private final BookingProperties props;

    @Override
    @Transactional
    public HoldResponse hold(String sessionId, HoldRequest request) {
        // Bỏ trùng và sắp tăng dần: các transaction khóa dòng cùng thứ tự thì không deadlock
        List<Long> seatIds = request.seatIds().stream().distinct().sorted().toList();
        if (seatIds.size() > props.maxSeatsPerBooking()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED,
                    "Mỗi đơn chỉ được chọn tối đa " + props.maxSeatsPerBooking() + " ghế");
        }

        Instant now = Instant.now();
        ShowtimeSummary showtime = showtimeService.getShowtimeSummary(request.showtimeId());
        checkOnSale(showtime, now);

        List<SeatResponse> seats = resolveSeats(showtime, seatIds);
        checkCouplePairs(seats, seatIds);

        Optional<Booking> previous = bookingRepository
                .findByGuestSessionIdAndShowtimeIdAndStatus(sessionId, showtime.id(), BookingStatus.HELD);
        if (previous.isPresent() && isSameActiveHold(previous.get(), seatIds, now)) {
            // Ghế đã đang được giữ rồi: báo lỗi như với người khác, không cấp lại thời gian
            throw new BusinessException(ErrorCode.SEAT_UNAVAILABLE, "Các ghế này đã có người giữ",
                    Map.of("seatIds", seatIds));
        }
        previous.ifPresent(old -> cancelHold(old, "Khách chọn lại ghế"));

        Booking booking = newBooking(sessionId, request, showtime, seats, now);
        bookingRepository.save(booking);
        historyRepository.save(new BookingStatusHistory(booking, null, BookingStatus.HELD, null, ACTOR));

        int held = showtimeSeatRepository.holdSeats(showtime.id(), seatIds, booking.getId());
        if (held != seatIds.size()) {
            Set<Long> heldIds = new HashSet<>(showtimeSeatRepository.findHeldSeatIds(booking.getId()));
            List<Long> lost = seatIds.stream().filter(id -> !heldIds.contains(id)).toList();
            throw new BusinessException(ErrorCode.SEAT_UNAVAILABLE, "Một số ghế không còn trống",
                    Map.of("seatIds", lost));
        }

        return toResponse(booking, now);
    }

    private static HoldResponse toResponse(Booking booking, Instant now) {
        List<Long> seatIds = booking.getSeats().stream().map(BookingSeat::getSeatId).sorted().toList();
        return new HoldResponse(booking.getId(), booking.getCode(), booking.getShowtimeId(), seatIds,
                booking.getHoldExpiresAt(), now);
    }

    /** Đơn đang giữ còn hạn và giữ đúng tập ghế yêu cầu (so theo tập hợp, không theo thứ tự). */
    private static boolean isSameActiveHold(Booking held, List<Long> seatIds, Instant now) {
        if (held.getHoldExpiresAt() == null || !held.getHoldExpiresAt().isAfter(now)) {
            return false;
        }
        Set<Long> heldSeats = held.getSeats().stream().map(BookingSeat::getSeatId).collect(Collectors.toSet());
        return heldSeats.equals(new HashSet<>(seatIds));
    }

    /** BR-08: chỉ bán suất đang ON_SALE và trước giờ chiếu một khoảng cấu hình (mặc định 15 phút). */
    private void checkOnSale(ShowtimeSummary showtime, Instant now) {
        if (showtime.status() != ShowtimeStatus.ON_SALE) {
            throw new BusinessException(ErrorCode.SHOWTIME_NOT_AVAILABLE, "Suất chiếu hiện không mở bán");
        }
        if (!now.isBefore(showtime.startAt().minus(props.salesCutoffBeforeStart()))) {
            throw new BusinessException(ErrorCode.SHOWTIME_NOT_AVAILABLE, "Suất chiếu đã ngừng bán vé");
        }
    }

    /** Mọi ghế phải là ghế bán được của đúng phòng chiếu suất này; trả ghế theo thứ tự seatIds. */
    private List<SeatResponse> resolveSeats(ShowtimeSummary showtime, List<Long> seatIds) {
        Map<Long, SeatResponse> byId = roomService.getRoomLayout(showtime.roomId()).seats().stream()
                .collect(Collectors.toMap(SeatResponse::id, Function.identity()));
        List<Long> invalid = seatIds.stream().filter(id -> !byId.containsKey(id)).toList();
        if (!invalid.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Ghế không thuộc phòng chiếu của suất này",
                    Map.of("seatIds", invalid));
        }
        return seatIds.stream().map(byId::get).toList();
    }

    /** BR-06: ghế đôi phải chọn cả hai ghế của cặp. */
    private void checkCouplePairs(List<SeatResponse> seats, List<Long> seatIds) {
        Set<Long> selected = new HashSet<>(seatIds);
        List<Long> broken = seats.stream()
                .filter(s -> s.type() == SeatType.COUPLE && (s.pairSeatId() == null || !selected.contains(s.pairSeatId())))
                .map(SeatResponse::id)
                .toList();
        if (!broken.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Ghế đôi phải chọn đủ cả cặp",
                    Map.of("seatIds", broken));
        }
    }

    /**
     * BR-04: mỗi phiên chỉ có một đơn HELD cho mỗi suất. Khách chọn tập ghế khác thì nhả ghế cũ và huỷ
     * đơn cũ; flush ngay để index unique uq_booking_one_hold không vướng khi chèn đơn mới.
     */
    private void cancelHold(Booking old, String reason) {
        showtimeSeatRepository.releaseSeats(old.getId());
        old.setStatus(BookingStatus.CANCELLED);
        old.setHoldExpiresAt(null);
        bookingRepository.saveAndFlush(old);
        historyRepository.save(new BookingStatusHistory(old, BookingStatus.HELD, BookingStatus.CANCELLED,
                reason, ACTOR));
    }

    @Override
    @Transactional
    public void release(String sessionId, Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .filter(b -> b.getGuestSessionId().equals(sessionId))
                .orElseThrow(() -> new BusinessException(ErrorCode.BOOKING_NOT_FOUND, "Không tìm thấy đơn đặt vé"));
        switch (booking.getStatus()) {
            case HELD -> cancelHold(booking, "Khách bỏ giữ ghế");
            case CANCELLED, EXPIRED -> { } // đã bỏ rồi: gọi lại vẫn thành công (idempotent)
            default -> throw new BusinessException(ErrorCode.CANCEL_NOT_ALLOWED,
                    "Đơn đang ở trạng thái " + booking.getStatus() + ", không thể bỏ giữ");
        }
    }

    /** Dựng booking HELD với giá từng ghế chốt tại thời điểm giữ (BR-12). */
    private Booking newBooking(String sessionId, HoldRequest request, ShowtimeSummary showtime,
                               List<SeatResponse> seats, Instant now) {
        var date = showtime.startAt().atZone(VN).toLocalDate();
        Booking booking = new Booking();
        booking.setCode(codeGenerator.next());
        booking.setGuestSessionId(sessionId);
        booking.setCustomerName(ContactNormalizer.name(request.customerName()));
        booking.setEmail(ContactNormalizer.email(request.email()));
        booking.setPhone(ContactNormalizer.phone(request.phone()));
        booking.setShowtimeId(showtime.id());   // phải set trước addSeat vì addSeat chép showtimeId
        booking.setStatus(BookingStatus.HELD);
        booking.setHoldExpiresAt(now.plus(props.holdDuration()));

        long subtotal = 0;
        for (SeatResponse s : seats) {
            long price = pricingService.priceOf(showtime.format(), s.type(), date);
            booking.addSeat(new BookingSeat(s.id(), s.type(), price));
            subtotal += price;
        }
        booking.setSubtotal(subtotal);
        booking.setDiscount(0);
        booking.setTotal(subtotal);
        return booking;
    }
}
