package com.smartparkinglot.domain.pricing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.smartparkinglot.domain.LoyaltyTier;
import com.smartparkinglot.domain.Money;
import com.smartparkinglot.domain.ParkingInterval;
import com.smartparkinglot.domain.VehicleClass;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class EarlyBirdPolicyTest {
    private static final ZoneId ZONE = ZoneId.of("America/Los_Angeles");
    private static final Instant ENTRY = Instant.parse("2026-11-23T14:00:00Z");
    private static final Instant EXIT = Instant.parse("2026-11-23T23:30:00Z");

    @Test
    void acceptsInclusiveWindowStartsAndAppliesLoyaltyAndVehicleMultipliers() {
        EarlyBirdPolicy policy = new EarlyBirdPolicy(ZONE, date -> false);

        assertEquals(
                Money.ofCents(1500),
                policy.quote(session(ENTRY, EXIT, Optional.empty())).orElseThrow().total());
        assertEquals(
                Money.ofCents(1200),
                policy.quote(session(ENTRY, EXIT, Optional.of(LoyaltyTier.GOLD)))
                        .orElseThrow().total());
        assertEquals(
                Money.ofCents(1050),
                policy.quote(session(ENTRY, EXIT, Optional.of(LoyaltyTier.PLATINUM)))
                        .orElseThrow().total());
        assertEquals(
                Money.ofCents(1200),
                policy.quote(session(ENTRY, EXIT, Optional.empty(), VehicleClass.MOTORCYCLE))
                        .orElseThrow().total());
        assertEquals(
                Money.ofCents(3000),
                policy.quote(session(ENTRY, EXIT, Optional.empty(), VehicleClass.BUS))
                        .orElseThrow().total());
    }

    @Test
    void enforcesExclusiveWindowEndsSameLocalDateAndEligibleWeekday() {
        EarlyBirdPolicy policy = new EarlyBirdPolicy(ZONE, date -> false);

        assertTrue(policy.quote(session(Instant.parse("2026-11-23T13:59:59Z"), EXIT,
                Optional.empty())).isEmpty());
        assertTrue(policy.quote(session(Instant.parse("2026-11-23T17:00:00Z"), EXIT,
                Optional.empty())).isEmpty());
        assertTrue(policy.quote(session(ENTRY, Instant.parse("2026-11-23T23:29:00Z"),
                Optional.empty())).isEmpty());
        assertTrue(policy.quote(session(ENTRY, Instant.parse("2026-11-24T03:00:00Z"),
                Optional.empty())).isEmpty());
        assertTrue(policy.quote(session(ENTRY, Instant.parse("2026-11-24T23:30:00Z"),
                Optional.empty())).isEmpty());

        EarlyBirdPolicy holidayPolicy = new EarlyBirdPolicy(
                ZONE, date -> Set.of(java.time.LocalDate.of(2026, 11, 23)).contains(date));
        assertTrue(holidayPolicy.quote(session(ENTRY, EXIT, Optional.empty())).isEmpty());
    }

    private static ParkingSession session(
            Instant entry, Instant exit, Optional<LoyaltyTier> loyaltyTier) {
        return session(entry, exit, loyaltyTier, VehicleClass.CAR);
    }

    private static ParkingSession session(
            Instant entry, Instant exit, Optional<LoyaltyTier> loyaltyTier,
            VehicleClass vehicleClass) {
        return new ParkingSession(
                new ParkingInterval(entry, exit), vehicleClass, loyaltyTier);
    }
}
