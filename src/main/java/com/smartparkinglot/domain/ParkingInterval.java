package com.smartparkinglot.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

public record ParkingInterval(Instant entryAt, Instant exitAt) {
    public ParkingInterval {
        Objects.requireNonNull(entryAt, "entryAt");
        Objects.requireNonNull(exitAt, "exitAt");
        if (!exitAt.isAfter(entryAt)) {
            throw new IllegalArgumentException("Exit instant must be after entry instant");
        }
    }

    public Duration duration() {
        return Duration.between(entryAt, exitAt);
    }
}
