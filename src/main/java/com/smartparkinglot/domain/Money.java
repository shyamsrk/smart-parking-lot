package com.smartparkinglot.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

public final class Money implements Comparable<Money> {
    private static final BigDecimal CENTS_PER_DOLLAR = BigDecimal.valueOf(100);

    private final long cents;

    private Money(long cents) {
        this.cents = cents;
    }

    public static Money ofCents(long cents) {
        if (cents < 0) {
            throw new IllegalArgumentException("Money cannot be negative");
        }
        return new Money(cents);
    }

    public static Money ofDollars(BigDecimal dollars) {
        Objects.requireNonNull(dollars, "dollars");
        if (dollars.signum() < 0) {
            throw new IllegalArgumentException("Money cannot be negative");
        }
        long roundedCents = dollars
                .multiply(CENTS_PER_DOLLAR)
                .setScale(0, RoundingMode.HALF_UP)
                .longValueExact();
        return ofCents(roundedCents);
    }

    public long cents() {
        return cents;
    }

    public BigDecimal asBigDecimal() {
        return BigDecimal.valueOf(cents, 2);
    }

    public Money plus(Money other) {
        Objects.requireNonNull(other, "other");
        return ofCents(Math.addExact(cents, other.cents));
    }

    @Override
    public int compareTo(Money other) {
        Objects.requireNonNull(other, "other");
        return Long.compare(cents, other.cents);
    }

    @Override
    public boolean equals(Object other) {
        return this == other || other instanceof Money money && cents == money.cents;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(cents);
    }

    @Override
    public String toString() {
        return asBigDecimal().toPlainString();
    }
}
