package com.cinebook.booking.repository;

import com.cinebook.booking.entity.BookingStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookingStatusHistoryRepository extends JpaRepository<BookingStatusHistory, Long> {
}
