package com.smartparkinglot.domain.pricing;

import com.smartparkinglot.domain.Money;
import java.time.Instant;
import java.util.Objects;

public record HourlySegment(Instant startsAt, Instant endsAt, Money amount) {
    public HourlySegment {
        Objects.requireNonNull(startsAt, "startsAt");
        Objects.requireNonNull(endsAt, "endsAt");
        Objects.requireNonNull(amount, "amount");
        if (!endsAt.isAfter(startsAt)) {
            throw new IllegalArgumentException("Segment end must follow its start");
        }
    }
}
