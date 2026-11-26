package com.smartparkinglot.domain;

import java.util.Objects;
import java.util.Optional;

public final class ParkingSpot {
    private final String id;
    private final SpotType type;
    private String occupiedByTicketId;

    public ParkingSpot(String id, SpotType type) {
        this.id = requireIdentifier(id, "id");
        this.type = Objects.requireNonNull(type, "type");
    }

    public String id() {
        return id;
    }

    public SpotType type() {
        return type;
    }

    public boolean isAvailable() {
        return occupiedByTicketId == null;
    }

    public Optional<String> occupiedByTicketId() {
        return Optional.ofNullable(occupiedByTicketId);
    }

    public void reserveFor(String ticketId, VehicleClass vehicleClass) {
        String validatedTicketId = requireIdentifier(ticketId, "ticketId");
        Objects.requireNonNull(vehicleClass, "vehicleClass");
        if (!type.supports(vehicleClass)) {
            throw new IllegalArgumentException(
                    "Spot type " + type + " does not support " + vehicleClass);
        }
        if (!isAvailable()) {
            throw new IllegalStateException("Spot is already occupied");
        }
        occupiedByTicketId = validatedTicketId;
    }

    public void release(String ticketId) {
        String validatedTicketId = requireIdentifier(ticketId, "ticketId");
        if (!validatedTicketId.equals(occupiedByTicketId)) {
            throw new IllegalStateException("Spot is not occupied by this ticket");
        }
        occupiedByTicketId = null;
    }

    private static String requireIdentifier(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " cannot be blank");
        }
        return value;
    }
}
