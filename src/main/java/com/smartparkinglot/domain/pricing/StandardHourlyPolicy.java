package com.smartparkinglot.domain.pricing;

import com.smartparkinglot.domain.Money;
import com.smartparkinglot.domain.ParkingInterval;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class StandardHourlyPolicy implements PricingPolicy {
    private static final BigDecimal PEAK_MULTIPLIER = new BigDecimal("1.5");

    private final ZoneId zoneId;
    private final HolidayCalendar holidayCalendar;

    public StandardHourlyPolicy(ZoneId zoneId, HolidayCalendar holidayCalendar) {
        this.zoneId = Objects.requireNonNull(zoneId, "zoneId");
        this.holidayCalendar = Objects.requireNonNull(holidayCalendar, "holidayCalendar");
    }

    @Override
    public PolicyType policyType() {
        return PolicyType.STANDARD_HOURLY;
    }

    @Override
    public Optional<PolicyQuote> quote(ParkingSession session) {
        Objects.requireNonNull(session, "session");
        ParkingInterval interval = session.interval();
        long billedHours = billedHours(interval.duration());
        List<HourlySegment> segments = new ArrayList<>();
        Money total = Money.ofCents(0);

        for (long block = 0; block < billedHours; block++) {
            Instant startsAt = interval.entryAt().plusSeconds(Math.multiplyExact(block, 3600));
            Instant endsAt = startsAt.plusSeconds(3600);
            boolean peak = overlapsEligiblePeakWindow(startsAt, endsAt);
            Money baseRate = baseRate(block);
            BigDecimal multiplier = session.vehicleClass().multiplier();
            if (peak) {
                multiplier = multiplier.multiply(PEAK_MULTIPLIER);
            }
            Money segmentAmount =
                    Money.ofDollars(baseRate.asBigDecimal().multiply(multiplier));
            segments.add(new HourlySegment(startsAt, endsAt, segmentAmount));
            total = total.plus(segmentAmount);
        }

        return Optional.of(new PolicyQuote(PolicyType.STANDARD_HOURLY, total, segments));
    }

    private boolean overlapsEligiblePeakWindow(Instant startsAt, Instant endsAt) {
        LocalDate date = startsAt.atZone(zoneId).toLocalDate();
        LocalDate lastDate = endsAt.minusNanos(1).atZone(zoneId).toLocalDate();
        while (!date.isAfter(lastDate)) {
            if (PricingTime.isEligibleWeekday(date, holidayCalendar)) {
                if (overlaps(startsAt, endsAt, date, 7, 0, 10, 0)
                        || overlaps(startsAt, endsAt, date, 16, 0, 19, 0)) {
                    return true;
                }
            }
            date = date.plusDays(1);
        }
        return false;
    }

    private boolean overlaps(
            Instant startsAt, Instant endsAt, LocalDate date,
            int startHour, int startMinute, int endHour, int endMinute) {
        Instant windowStartsAt = date.atTime(startHour, startMinute).atZone(zoneId).toInstant();
        Instant windowEndsAt = date.atTime(endHour, endMinute).atZone(zoneId).toInstant();
        return startsAt.isBefore(windowEndsAt) && endsAt.isAfter(windowStartsAt);
    }

    private static long billedHours(Duration duration) {
        long completeHours = duration.toHours();
        return duration.minusHours(completeHours).isZero()
                ? completeHours
                : Math.addExact(completeHours, 1);
    }

    private static Money baseRate(long block) {
        return switch ((int) Math.min(block, 2)) {
            case 0 -> Money.ofCents(500);
            case 1 -> Money.ofCents(300);
            default -> Money.ofCents(200);
        };
    }
}
