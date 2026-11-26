package com.smartparkinglot.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class ParkingTicketTest {
    private static final Instant ENTRY = Instant.parse("2026-11-26T08:00:00Z");

    @Test
    void closesWithValidExitInstant() {
        ParkingTicket ticket =
                new ParkingTicket("T-1", VehicleClass.CAR, ENTRY, "C-1");
        Instant exit = ENTRY.plusSeconds(3600);

        ticket.close(exit);

        assertEquals(TicketState.CLOSED, ticket.state());
        assertEquals(exit, ticket.exitAt().orElseThrow());
    }

    @Test
    void invalidOrRepeatedCloseLeavesTicketUnchanged() {
        ParkingTicket ticket =
                new ParkingTicket("T-1", VehicleClass.CAR, ENTRY, "C-1");

        assertThrows(IllegalArgumentException.class, () -> ticket.close(ENTRY));
        assertEquals(TicketState.ACTIVE, ticket.state());
        assertTrue(ticket.exitAt().isEmpty());

        Instant exit = ENTRY.plusSeconds(3600);
        ticket.close(exit);
        assertThrows(IllegalStateException.class, () -> ticket.close(exit));
        assertEquals(exit, ticket.exitAt().orElseThrow());
    }
}
