package com.smartparkinglot.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class ParkingIntervalTest {
    @Test
    void acceptsPositiveElapsedTime() {
        ParkingInterval interval = new ParkingInterval(
                Instant.parse("2026-03-08T06:30:00Z"),
                Instant.parse("2026-03-08T07:30:00Z"));

        assertEquals(Duration.ofHours(1), interval.duration());
    }

    @Test
    void rejectsEqualOrReversedInstants() {
        Instant entry = Instant.parse("2026-03-08T06:30:00Z");

        assertThrows(
                IllegalArgumentException.class,
                () -> new ParkingInterval(entry, entry));
        assertThrows(
                IllegalArgumentException.class,
                () -> new ParkingInterval(entry.plusSeconds(1), entry));
    }
}
