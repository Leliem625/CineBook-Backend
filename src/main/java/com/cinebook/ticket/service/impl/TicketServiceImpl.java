package com.cinebook.ticket.service.impl;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cinebook.ticket.entity.Ticket;
import com.cinebook.ticket.repository.TicketRepository;
import com.cinebook.ticket.service.QrTokenService;
import com.cinebook.ticket.service.TicketService;

import lombok.RequiredArgsConstructor;

/** Cài đặt {@link TicketService}. */
@Service
@RequiredArgsConstructor
public class TicketServiceImpl implements TicketService {

    private final TicketRepository ticketRepository;
    private final QrTokenService qrTokenService;

    @Override
    @Transactional
    public void issueTickets(Long bookingId, Long showtimeId, List<Long> seatIds) {
        List<Ticket> tickets = seatIds.stream()
                .map(seatId -> new Ticket(bookingId, showtimeId, seatId, qrTokenService.newToken()))
                .toList();
        ticketRepository.saveAll(tickets);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, String> getQrTokens(Long bookingId) {
        return ticketRepository.findByBookingId(bookingId).stream()
                .collect(Collectors.toMap(Ticket::getSeatId, Ticket::getQrToken));
    }
}
