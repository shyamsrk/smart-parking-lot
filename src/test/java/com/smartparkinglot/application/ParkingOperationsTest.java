package com.smartparkinglot.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.smartparkinglot.domain.ParkingFacility;
import com.smartparkinglot.domain.ParkingLevel;
import com.smartparkinglot.domain.ParkingSpot;
import com.smartparkinglot.domain.SpotType;
import com.smartparkinglot.domain.TicketState;
import com.smartparkinglot.domain.VehicleClass;
import com.smartparkinglot.domain.pricing.BestFareCalculator;
import com.smartparkinglot.domain.pricing.EarlyBirdPolicy;
import com.smartparkinglot.domain.pricing.HolidayCalendar;
import com.smartparkinglot.domain.pricing.NightOwlPolicy;
import com.smartparkinglot.domain.pricing.PolicyType;
import com.smartparkinglot.domain.pricing.StandardHourlyPolicy;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ParkingOperationsTest {
    private static final ZoneId ZONE = ZoneId.of("America/Los_Angeles");
    private static final Instant ENTRY = Instant.parse("2026-11-23T19:00:00Z");
    private static final Instant EXIT = Instant.parse("2026-11-23T20:00:00Z");
    private static final HolidayCalendar NO_HOLIDAYS = date -> false;

    @Test
    void assignsFirstCompatibleSpotAndPersistsActiveTicket() {
        ParkingSpot large = new ParkingSpot("L-1", SpotType.LARGE);
        ParkingSpot firstCompact = new ParkingSpot("C-1", SpotType.COMPACT);
        ParkingSpot secondCompact = new ParkingSpot("C-2", SpotType.COMPACT);
        Fixture fixture = fixture(List.of(large, firstCompact, secondCompact));

        var ticket = fixture.operations().issueTicket("T-1", VehicleClass.CAR, ENTRY);

        assertEquals("C-1", ticket.assignedSpotId());
        assertEquals(TicketState.ACTIVE, ticket.state());
        assertEquals("T-1", firstCompact.occupiedByTicketId().orElseThrow());
        assertTrue(large.isAvailable());
        assertTrue(secondCompact.isAvailable());
        assertTrue(fixture.ticketStore().find("T-1").isPresent());
    }

    @Test
    void rejectsFullFacilityWithoutCreatingTicketOrChangingOccupancy() {
        ParkingSpot compact = new ParkingSpot("C-1", SpotType.COMPACT);
        Fixture fixture = fixture(List.of(compact));

        assertThrows(
                NoCompatibleSpotException.class,
                () -> fixture.operations().issueTicket("T-1", VehicleClass.BUS, ENTRY));

        assertTrue(compact.isAvailable());
        assertTrue(fixture.ticketStore().find("T-1").isEmpty());
        assertEquals(0, fixture.ticketStore().size());
    }

    @Test
    void quoteDoesNotCloseTicketOrReleaseSpot() {
        ParkingSpot spot = new ParkingSpot("C-1", SpotType.COMPACT);
        Fixture fixture = fixture(List.of(spot));
        var ticket = fixture.operations().issueTicket("T-1", VehicleClass.CAR, ENTRY);

        var comparison = fixture.operations().quoteExit("T-1", EXIT, Optional.empty());

        assertEquals(PolicyType.STANDARD_HOURLY, comparison.winningQuote().policyType());
        assertEquals(TicketState.ACTIVE, ticket.state());
        assertFalse(spot.isAvailable());
        assertEquals("T-1", spot.occupiedByTicketId().orElseThrow());
    }

    @Test
    void completesExitByClosingTicketAndReleasingAssignedSpot() {
        ParkingSpot spot = new ParkingSpot("C-1", SpotType.COMPACT);
        Fixture fixture = fixture(List.of(spot));
        var ticket = fixture.operations().issueTicket("T-1", VehicleClass.CAR, ENTRY);

        fixture.operations().completeExit("T-1", EXIT);

        assertEquals(TicketState.CLOSED, ticket.state());
        assertEquals(EXIT, ticket.exitAt().orElseThrow());
        assertTrue(spot.isAvailable());
    }

    @Test
    void unknownTicketFailsWithoutChangingFacilityState() {
        ParkingSpot spot = new ParkingSpot("C-1", SpotType.COMPACT);
        Fixture fixture = fixture(List.of(spot));
        fixture.operations().issueTicket("T-1", VehicleClass.CAR, ENTRY);

        assertThrows(
                TicketNotFoundException.class,
                () -> fixture.operations().quoteExit("missing", EXIT, Optional.empty()));
        assertThrows(
                TicketNotFoundException.class,
                () -> fixture.operations().completeExit("missing", EXIT));

        assertEquals("T-1", spot.occupiedByTicketId().orElseThrow());
        assertEquals(1, fixture.ticketStore().size());
    }

    @Test
    void invalidQuoteAndCompletionTimestampsLeaveStateUnchanged() {
        ParkingSpot spot = new ParkingSpot("C-1", SpotType.COMPACT);
        Fixture fixture = fixture(List.of(spot));
        var ticket = fixture.operations().issueTicket("T-1", VehicleClass.CAR, ENTRY);

        assertThrows(
                IllegalArgumentException.class,
                () -> fixture.operations().quoteExit("T-1", ENTRY, Optional.empty()));
        assertThrows(
                IllegalArgumentException.class,
                () -> fixture.operations().quoteExit(
                        "T-1", ENTRY.minusSeconds(1), Optional.empty()));
        assertThrows(
                IllegalArgumentException.class,
                () -> fixture.operations().completeExit("T-1", ENTRY));
        assertThrows(
                IllegalArgumentException.class,
                () -> fixture.operations().completeExit("T-1", ENTRY.minusSeconds(1)));
        assertThrows(
                IllegalArgumentException.class,
                () -> fixture.operations().issueTicket("T-2", VehicleClass.CAR, null));
        assertThrows(
                IllegalArgumentException.class,
                () -> fixture.operations().quoteExit("T-1", null, Optional.empty()));
        assertThrows(
                IllegalArgumentException.class,
                () -> fixture.operations().completeExit("T-1", null));

        assertEquals(TicketState.ACTIVE, ticket.state());
        assertTrue(ticket.exitAt().isEmpty());
        assertFalse(spot.isAvailable());
        assertEquals("T-1", spot.occupiedByTicketId().orElseThrow());
        assertTrue(fixture.ticketStore().find("T-2").isEmpty());
    }

    @Test
    void rejectsClosedTicketAndDoesNotReleaseAnotherTicketSpot() {
        ParkingSpot first = new ParkingSpot("C-1", SpotType.COMPACT);
        ParkingSpot second = new ParkingSpot("C-2", SpotType.COMPACT);
        Fixture fixture = fixture(List.of(first, second));
        fixture.operations().issueTicket("T-1", VehicleClass.CAR, ENTRY);
        var activeTicket = fixture.operations().issueTicket(
                "T-2", VehicleClass.CAR, ENTRY.plusSeconds(60));

        fixture.operations().completeExit("T-1", EXIT);
        assertThrows(
                IllegalStateException.class,
                () -> fixture.operations().quoteExit(
                        "T-1", EXIT.plusSeconds(60), Optional.empty()));
        assertThrows(
                IllegalStateException.class,
                () -> fixture.operations().completeExit("T-1", EXIT.plusSeconds(60)));

        assertEquals(TicketState.ACTIVE, activeTicket.state());
        assertEquals("T-2", second.occupiedByTicketId().orElseThrow());
    }

    private static Fixture fixture(List<ParkingSpot> spots) {
        ParkingFacility facility = new ParkingFacility(
                "MAIN",
                ZONE,
                List.of(new ParkingLevel("1", spots)));
        InMemoryTicketStore ticketStore = new InMemoryTicketStore();
        BestFareCalculator calculator = new BestFareCalculator(List.of(
                new StandardHourlyPolicy(ZONE, NO_HOLIDAYS),
                new EarlyBirdPolicy(ZONE, NO_HOLIDAYS),
                new NightOwlPolicy(ZONE, NO_HOLIDAYS)));
        return new Fixture(
                new ParkingOperations(facility, ticketStore, calculator), ticketStore);
    }

    private record Fixture(
            ParkingOperations operations, InMemoryTicketStore ticketStore) {
    }
}
