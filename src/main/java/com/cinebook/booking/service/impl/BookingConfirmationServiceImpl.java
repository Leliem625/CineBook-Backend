package com.cinebook.booking.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cinebook.booking.entity.Booking;
import com.cinebook.booking.entity.BookingSeat;
import com.cinebook.booking.entity.BookingStatus;
import com.cinebook.booking.entity.BookingStatusHistory;
import com.cinebook.booking.repository.BookingRepository;
import com.cinebook.booking.repository.BookingStatusHistoryRepository;
import com.cinebook.booking.service.BookingConfirmationService;
import com.cinebook.showtime.repository.ShowtimeSeatRepository;
import com.cinebook.ticket.service.TicketService;

import lombok.RequiredArgsConstructor;

/** Cài đặt {@link BookingConfirmationService}. */
@Service
@RequiredArgsConstructor
public class BookingConfirmationServiceImpl implements BookingConfirmationService {

    private static final String ACTOR = "system";

    private final BookingRepository bookingRepository;
    private final BookingStatusHistoryRepository historyRepository;
    private final ShowtimeSeatRepository showtimeSeatRepository;
    private final TicketService ticketService;

    @Override
    @Transactional
    public boolean confirm(Long bookingId) {
        // Đổi trạng thái trước (UPDATE có điều kiện), rồi mới nạp entity để không dính bản cũ trong cache
        if (bookingRepository.markConfirmed(bookingId) == 0) {
            return false;
        }
        Booking booking = bookingRepository.findById(bookingId).orElseThrow();
        historyRepository.save(new BookingStatusHistory(booking, BookingStatus.PAYMENT_PENDING,
                BookingStatus.CONFIRMED, "Thanh toán thành công", ACTOR));

        int sold = showtimeSeatRepository.sellSeats(bookingId);
        if (sold != booking.getSeats().size()) {
            // Không được xảy ra (ghế của đơn PAYMENT_PENDING luôn HELD): rollback để không xác nhận dở dang
            throw new IllegalStateException("Đơn " + booking.getCode() + ": bán được " + sold + "/"
                    + booking.getSeats().size() + " ghế");
        }
        ticketService.issueTickets(bookingId, booking.getShowtimeId(),
                booking.getSeats().stream().map(BookingSeat::getSeatId).sorted().toList());
        return true;
    }

    @Override
    @Transactional
    public void paymentFailed(Long bookingId) {
        if (bookingRepository.revertToHeld(bookingId) == 1) {
            Booking booking = bookingRepository.findById(bookingId).orElseThrow();
            historyRepository.save(new BookingStatusHistory(booking, BookingStatus.PAYMENT_PENDING,
                    BookingStatus.HELD, "Thanh toán không thành công", ACTOR));
        }
    }

    @Override
    @Transactional
    public void expireUnpaid(Long bookingId) {
        if (bookingRepository.markExpiredUnpaid(bookingId) == 1) {
            Booking booking = bookingRepository.findById(bookingId).orElseThrow();
            showtimeSeatRepository.releaseSeats(bookingId);
            historyRepository.save(new BookingStatusHistory(booking, BookingStatus.PAYMENT_PENDING,
                    BookingStatus.EXPIRED, "Quá hạn thanh toán", ACTOR));
        }
    }
}
