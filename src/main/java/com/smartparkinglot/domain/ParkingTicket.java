package com.smartparkinglot.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

public final class ParkingTicket {
    private final String id;
    private final VehicleClass vehicleClass;
    private final Instant entryAt;
    private final String assignedSpotId;
    private TicketState state = TicketState.ACTIVE;
    private Instant exitAt;

    public ParkingTicket(
            String id, VehicleClass vehicleClass, Instant entryAt, String assignedSpotId) {
        this.id = requireIdentifier(id, "id");
        this.vehicleClass = Objects.requireNonNull(vehicleClass, "vehicleClass");
        this.entryAt = Objects.requireNonNull(entryAt, "entryAt");
        this.assignedSpotId = requireIdentifier(assignedSpotId, "assignedSpotId");
    }

    public String id() {
        return id;
    }

    public VehicleClass vehicleClass() {
        return vehicleClass;
    }

    public Instant entryAt() {
        return entryAt;
    }

    public String assignedSpotId() {
        return assignedSpotId;
    }

    public TicketState state() {
        return state;
    }

    public Optional<Instant> exitAt() {
        return Optional.ofNullable(exitAt);
    }

    public void close(Instant exitAt) {
        if (state != TicketState.ACTIVE) {
            throw new IllegalStateException("Ticket is already closed");
        }
        ParkingInterval interval = new ParkingInterval(entryAt, exitAt);
        this.exitAt = interval.exitAt();
        this.state = TicketState.CLOSED;
    }

    private static String requireIdentifier(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " cannot be blank");
        }
        return value;
    }
}
