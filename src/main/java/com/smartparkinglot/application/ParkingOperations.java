package com.smartparkinglot.application;

import com.smartparkinglot.domain.ParkingFacility;
import com.smartparkinglot.domain.ParkingInterval;
import com.smartparkinglot.domain.ParkingSpot;
import com.smartparkinglot.domain.ParkingTicket;
import com.smartparkinglot.domain.TicketState;
import com.smartparkinglot.domain.VehicleClass;
import com.smartparkinglot.domain.LoyaltyTier;
import com.smartparkinglot.domain.pricing.BestFareCalculator;
import com.smartparkinglot.domain.pricing.FareComparison;
import com.smartparkinglot.domain.pricing.ParkingSession;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

public final class ParkingOperations {
    private final ParkingFacility facility;
    private final InMemoryTicketStore ticketStore;
    private final BestFareCalculator bestFareCalculator;

    public ParkingOperations(
            ParkingFacility facility,
            InMemoryTicketStore ticketStore,
            BestFareCalculator bestFareCalculator) {
        this.facility = Objects.requireNonNull(facility, "facility");
        this.ticketStore = Objects.requireNonNull(ticketStore, "ticketStore");
        this.bestFareCalculator =
                Objects.requireNonNull(bestFareCalculator, "bestFareCalculator");
    }

    public synchronized ParkingTicket issueTicket(
            String ticketId, VehicleClass vehicleClass, Instant entryAt) {
        Objects.requireNonNull(vehicleClass, "vehicleClass");
        requireTimestamp(entryAt, "entryAt");
        if (ticketId == null || ticketId.isBlank()) {
            throw new IllegalArgumentException("ticketId cannot be blank");
        }
        if (ticketStore.find(ticketId).isPresent()) {
            throw new IllegalArgumentException("Ticket identifier already exists: " + ticketId);
        }

        ParkingSpot spot = facility.spots().stream()
                .filter(ParkingSpot::isAvailable)
                .filter(candidate -> candidate.type().supports(vehicleClass))
                .findFirst()
                .orElseThrow(() -> new NoCompatibleSpotException(
                        "No compatible available spot for " + vehicleClass));

        ParkingTicket ticket =
                new ParkingTicket(ticketId, vehicleClass, entryAt, spot.id());
        spot.reserveFor(ticketId, vehicleClass);
        ticketStore.save(ticket);
        return ticket;
    }

    public synchronized FareComparison quoteExit(
            String ticketId, Instant exitAt, Optional<LoyaltyTier> loyaltyTier) {
        ParkingTicket ticket = findActiveTicket(ticketId);
        requireTimestamp(exitAt, "exitAt");
        ParkingInterval interval = new ParkingInterval(ticket.entryAt(), exitAt);
        ParkingSession session = new ParkingSession(
                interval, ticket.vehicleClass(), Objects.requireNonNull(loyaltyTier, "loyaltyTier"));
        return bestFareCalculator.calculate(session);
    }

    public synchronized void completeExit(String ticketId, Instant exitAt) {
        ParkingTicket ticket = findActiveTicket(ticketId);
        requireTimestamp(exitAt, "exitAt");
        ParkingInterval interval = new ParkingInterval(ticket.entryAt(), exitAt);
        ParkingSpot spot = facility.spots().stream()
                .filter(candidate -> candidate.id().equals(ticket.assignedSpotId()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Assigned parking spot does not exist: " + ticket.assignedSpotId()));
        if (spot.occupiedByTicketId().filter(ticket.id()::equals).isEmpty()) {
            throw new IllegalStateException("Assigned spot is not occupied by the active ticket");
        }

        ticket.close(interval.exitAt());
        spot.release(ticket.id());
    }

    private ParkingTicket findActiveTicket(String ticketId) {
        Objects.requireNonNull(ticketId, "ticketId");
        ParkingTicket ticket = ticketStore.find(ticketId)
                .orElseThrow(() -> new TicketNotFoundException(ticketId));
        if (ticket.state() != TicketState.ACTIVE) {
            throw new IllegalStateException("Ticket is already closed: " + ticketId);
        }
        return ticket;
    }

    private static void requireTimestamp(Instant timestamp, String name) {
        if (timestamp == null) {
            throw new IllegalArgumentException(name + " is required");
        }
    }
}
