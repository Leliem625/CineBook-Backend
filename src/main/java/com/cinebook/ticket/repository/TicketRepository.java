package com.cinebook.ticket.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cinebook.ticket.entity.Ticket;

public interface TicketRepository extends JpaRepository<Ticket, Long> {

    List<Ticket> findByBookingId(Long bookingId);
}
