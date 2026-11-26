package com.smartparkinglot.domain;

import java.time.ZoneId;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class ParkingFacility {
    private final String id;
    private final ZoneId timeZone;
    private final List<ParkingLevel> levels;
    private final List<ParkingSpot> spots;

    public ParkingFacility(String id, ZoneId timeZone, List<ParkingLevel> levels) {
        this.id = requireIdentifier(id, "id");
        this.timeZone = Objects.requireNonNull(timeZone, "timeZone");
        this.levels = List.copyOf(Objects.requireNonNull(levels, "levels"));

        Set<String> levelIds = new HashSet<>();
        Set<String> spotIds = new HashSet<>();
        for (ParkingLevel level : this.levels) {
            Objects.requireNonNull(level, "level");
            if (!levelIds.add(level.id())) {
                throw new IllegalArgumentException(
                        "Duplicate level identifier: " + level.id());
            }
            for (ParkingSpot spot : level.spots()) {
                if (!spotIds.add(spot.id())) {
                    throw new IllegalArgumentException(
                            "Duplicate spot identifier: " + spot.id());
                }
            }
        }
        this.spots = this.levels.stream()
                .flatMap(level -> level.spots().stream())
                .toList();
    }

    public String id() {
        return id;
    }

    public ZoneId timeZone() {
        return timeZone;
    }

    public List<ParkingLevel> levels() {
        return levels;
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
