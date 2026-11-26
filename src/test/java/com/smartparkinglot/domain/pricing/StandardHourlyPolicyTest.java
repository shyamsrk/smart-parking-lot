package com.smartparkinglot.domain.pricing;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.smartparkinglot.domain.Money;
import com.smartparkinglot.domain.ParkingInterval;
import com.smartparkinglot.domain.VehicleClass;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Set;
import org.junit.jupiter.api.Test;

class StandardHourlyPolicyTest {
    private static final ZoneId ZONE = ZoneId.of("America/Los_Angeles");
    private static final HolidayCalendar NO_HOLIDAYS = date -> false;

    @Test
    void roundsElapsedDurationUpAndUsesProgressiveRates() {
        StandardHourlyPolicy policy = new StandardHourlyPolicy(ZONE, NO_HOLIDAYS);
        Instant entry = Instant.parse("2026-11-23T19:00:00Z");

        assertQuote(policy, session(entry, entry.plusSeconds(1800), VehicleClass.CAR), 500, 1);
        assertQuote(policy, session(entry, entry.plusSeconds(3600), VehicleClass.CAR), 500, 1);
        assertQuote(policy, session(entry, entry.plusSeconds(3601), VehicleClass.CAR), 800, 2);
        assertQuote(policy, session(entry, entry.plusSeconds(7201), VehicleClass.CAR), 1000, 3);
    }

    @Test
    void pricesFloatingSegmentsThatOverlapWeekdayPeakWindows() {
        StandardHourlyPolicy policy = new StandardHourlyPolicy(ZONE, NO_HOLIDAYS);
        Instant entry = Instant.parse("2026-11-23T14:30:00Z");
        PolicyQuote quote = policy.quote(
                session(entry, entry.plusSeconds(7200), VehicleClass.CAR)).orElseThrow();

        assertEquals(Money.ofCents(1200), quote.total());
        assertEquals(2, quote.hourlyBreakdown().size());
        assertEquals(Money.ofCents(750), quote.hourlyBreakdown().get(0).amount());
        assertEquals(Money.ofCents(450), quote.hourlyBreakdown().get(1).amount());
    }

    @Test
    void observesInclusivePeakStartAndExclusivePeakEnd() {
        StandardHourlyPolicy policy = new StandardHourlyPolicy(ZONE, NO_HOLIDAYS);
        Instant peakStart = Instant.parse("2026-11-23T15:00:00Z");
        Instant peakEnd = Instant.parse("2026-11-23T18:00:00Z");
        Instant justBefore = Instant.parse("2026-11-23T14:00:00Z");

        assertQuote(
                policy,
                session(justBefore, peakStart, VehicleClass.CAR),
                500,
                1);
        assertQuote(
                policy,
                session(peakEnd, peakEnd.plusSeconds(3600), VehicleClass.CAR),
                500,
                1);
        assertQuote(
                policy,
                session(peakStart, peakStart.plusSeconds(3600), VehicleClass.CAR),
                750,
                1);
    }

    @Test
    void skipsPeakSurchargeOnWeekendsAndHolidaysAndAppliesVehicleMultiplier() {
        Instant saturdayPeak = Instant.parse("2026-11-28T17:00:00Z");
        Instant mondayPeak = Instant.parse("2026-11-23T17:00:00Z");
        HolidayCalendar mondayHoliday = date ->
                Set.of(java.time.LocalDate.of(2026, 11, 23)).contains(date);

        assertQuote(
                new StandardHourlyPolicy(ZONE, NO_HOLIDAYS),
                session(saturdayPeak, saturdayPeak.plusSeconds(3600), VehicleClass.CAR),
                500,
                1);
        assertQuote(
                new StandardHourlyPolicy(ZONE, mondayHoliday),
                session(mondayPeak, mondayPeak.plusSeconds(3600), VehicleClass.CAR),
                500,
                1);
        assertQuote(
                new StandardHourlyPolicy(ZONE, NO_HOLIDAYS),
                session(saturdayPeak, saturdayPeak.plusSeconds(3600), VehicleClass.MOTORCYCLE),
                400,
                1);
        assertQuote(
                new StandardHourlyPolicy(ZONE, NO_HOLIDAYS),
                session(saturdayPeak, saturdayPeak.plusSeconds(3600), VehicleClass.BUS),
                1000,
                1);
    }

    @Test
    void measuresHourlyBlocksAsElapsedTimeAcrossDaylightSavingJump() {
        Instant entry = Instant.parse("2026-03-08T09:30:00Z");
        StandardHourlyPolicy policy = new StandardHourlyPolicy(ZONE, NO_HOLIDAYS);
        PolicyQuote quote = policy.quote(
                session(entry, Instant.parse("2026-03-08T10:30:00Z"), VehicleClass.CAR))
                .orElseThrow();

        assertEquals(Money.ofCents(500), quote.total());
        assertEquals(1, quote.hourlyBreakdown().size());
        assertEquals(Instant.parse("2026-03-08T10:30:00Z"),
                quote.hourlyBreakdown().get(0).endsAt());
    }

    @Test
    void measuresHourlyBlocksAsElapsedTimeAcrossRepeatedLocalHour() {
        Instant entry = Instant.parse("2026-11-01T08:30:00Z");
        StandardHourlyPolicy policy = new StandardHourlyPolicy(ZONE, NO_HOLIDAYS);
        PolicyQuote quote = policy.quote(
                session(entry, Instant.parse("2026-11-01T09:30:00Z"), VehicleClass.CAR))
                .orElseThrow();

        assertEquals(Money.ofCents(500), quote.total());
        assertEquals(1, quote.hourlyBreakdown().size());
        assertEquals(Instant.parse("2026-11-01T09:30:00Z"),
                quote.hourlyBreakdown().get(0).endsAt());
    }

    private static ParkingSession session(
            Instant entry, Instant exit, VehicleClass vehicleClass) {
        return new ParkingSession(
                new ParkingInterval(entry, exit), vehicleClass, java.util.Optional.empty());
    }

    private static void assertQuote(
            StandardHourlyPolicy policy, ParkingSession session, long expectedCents,
            int expectedSegments) {
        PolicyQuote quote = policy.quote(session).orElseThrow();
        assertEquals(Money.ofCents(expectedCents), quote.total());
        assertEquals(expectedSegments, quote.hourlyBreakdown().size());
    }
}
