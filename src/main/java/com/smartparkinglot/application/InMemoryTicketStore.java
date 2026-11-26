package com.smartparkinglot.application;

import com.smartparkinglot.domain.ParkingTicket;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class InMemoryTicketStore {
    private final Map<String, ParkingTicket> tickets = new HashMap<>();

    public synchronized Optional<ParkingTicket> find(String ticketId) {
        return Optional.ofNullable(tickets.get(Objects.requireNonNull(ticketId, "ticketId")));
    }

    synchronized void save(ParkingTicket ticket) {
        Objects.requireNonNull(ticket, "ticket");
        if (tickets.putIfAbsent(ticket.id(), ticket) != null) {
            throw new IllegalArgumentException("Ticket identifier already exists: " + ticket.id());
        }
    }

    public synchronized int size() {
        return tickets.size();
    }
}
