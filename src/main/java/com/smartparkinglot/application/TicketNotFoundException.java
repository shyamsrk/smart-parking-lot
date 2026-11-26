package com.smartparkinglot.application;

public final class TicketNotFoundException extends RuntimeException {
    public TicketNotFoundException(String ticketId) {
        super("Ticket not found: " + ticketId);
    }
}
