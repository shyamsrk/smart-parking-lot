package com.smartparkinglot.domain;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class ParkingLevel {
    private final String id;
    private final List<ParkingSpot> spots;

    public ParkingLevel(String id, List<ParkingSpot> spots) {
        this.id = requireIdentifier(id, "id");
        this.spots = List.copyOf(Objects.requireNonNull(spots, "spots"));
        Set<String> spotIds = new HashSet<>();
        for (ParkingSpot spot : this.spots) {
            Objects.requireNonNull(spot, "spot");
            if (!spotIds.add(spot.id())) {
                throw new IllegalArgumentException("Duplicate spot identifier: " + spot.id());
            }
        }
    }

    public String id() {
        return id;
    }

    public List<ParkingSpot> spots() {
        return spots;
    }

    private static String requireIdentifier(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " cannot be blank");
        }
        return value;
    }
}
