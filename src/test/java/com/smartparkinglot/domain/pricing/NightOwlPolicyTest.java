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

class NightOwlPolicyTest {
    private static final ZoneId ZONE = ZoneId.of("America/Los_Angeles");
    private static final Instant ENTRY = Instant.parse("2026-11-24T02:00:00Z");
    private static final Instant EXIT = Instant.parse("2026-11-24T13:00:00Z");

    @Test
    void acceptsWindowStartsAndAppliesDiscountAndVehicleMultipliers() {
        NightOwlPolicy policy = new NightOwlPolicy(ZONE, date -> false);

        assertEquals(
                Money.ofCents(800),
                policy.quote(session(ENTRY, EXIT, Optional.empty())).orElseThrow().total());
        assertEquals(
                Money.ofCents(560),
                policy.quote(session(ENTRY, EXIT, Optional.of(LoyaltyTier.PLATINUM)))
                        .orElseThrow().total());
        assertEquals(
                Money.ofCents(640),
                policy.quote(session(ENTRY, EXIT, Optional.empty(), VehicleClass.MOTORCYCLE))
                        .orElseThrow().total());
        assertEquals(
                Money.ofCents(1600),
                policy.quote(session(ENTRY, EXIT, Optional.empty(), VehicleClass.BUS))
                        .orElseThrow().total());
    }

    @Test
    void requiresConsecutiveEligibleWeekdaysAndExclusiveExitEnd() {
        NightOwlPolicy policy = new NightOwlPolicy(ZONE, date -> false);

        assertTrue(policy.quote(session(Instant.parse("2026-11-24T01:59:59Z"), EXIT,
                Optional.empty())).isEmpty());
        assertTrue(policy.quote(session(ENTRY, Instant.parse("2026-11-24T18:00:00Z"),
                Optional.empty())).isEmpty());
        assertTrue(policy.quote(session(ENTRY, Instant.parse("2026-11-25T13:00:00Z"),
                Optional.empty())).isEmpty());

        NightOwlPolicy holidayPolicy = new NightOwlPolicy(
                ZONE, date -> Set.of(java.time.LocalDate.of(2026, 11, 24)).contains(date));
        assertTrue(holidayPolicy.quote(session(ENTRY, EXIT, Optional.empty())).isEmpty());

        Instant fridayEntry = Instant.parse("2026-11-28T02:00:00Z");
        Instant saturdayExit = Instant.parse("2026-11-28T13:00:00Z");
        assertTrue(policy.quote(session(fridayEntry, saturdayExit, Optional.empty())).isEmpty());
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
