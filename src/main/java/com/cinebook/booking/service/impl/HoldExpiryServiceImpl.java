package com.cinebook.booking.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cinebook.booking.entity.Booking;
import com.cinebook.booking.entity.BookingStatus;
import com.cinebook.booking.entity.BookingStatusHistory;
import com.cinebook.booking.repository.BookingRepository;
import com.cinebook.booking.repository.BookingStatusHistoryRepository;
import com.cinebook.booking.service.HoldExpiryService;
import com.cinebook.showtime.repository.ShowtimeSeatRepository;

import lombok.RequiredArgsConstructor;

/**
 * Cài đặt {@link HoldExpiryService}. Mỗi lô là một transaction nên lỗi ở lô sau không làm mất lô trước.
 * Đơn bị khóa dòng (FOR UPDATE) từ lúc chọn tới khi commit, nên nếu khách đang chuyển đơn sang
 * PAYMENT_PENDING cùng lúc thì một bên chờ bên kia và chỉ một bên thắng.
 */
@Service
@RequiredArgsConstructor
public class HoldExpiryServiceImpl implements HoldExpiryService {

    private final BookingRepository bookingRepository;
    private final BookingStatusHistoryRepository historyRepository;
    private final ShowtimeSeatRepository showtimeSeatRepository;

    @Override
    @Transactional
    public int expireOverdue(int batchSize) {
        List<Long> ids = bookingRepository.lockOverdueHeld(batchSize);
        for (Booking booking : bookingRepository.findAllById(ids)) {
            showtimeSeatRepository.releaseSeats(booking.getId());
            booking.setStatus(BookingStatus.EXPIRED); // giữ nguyên holdExpiresAt để tra cứu
            historyRepository.save(new BookingStatusHistory(booking, BookingStatus.HELD, BookingStatus.EXPIRED,
                    "Hết hạn giữ ghế", "system"));
        }
        return ids.size();
    }
}
