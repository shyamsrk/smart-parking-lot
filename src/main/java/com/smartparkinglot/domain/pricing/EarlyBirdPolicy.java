package com.smartparkinglot.domain.pricing;

import com.smartparkinglot.domain.LoyaltyTier;
import com.smartparkinglot.domain.Money;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class EarlyBirdPolicy implements PricingPolicy {
    private static final Money BASE_FEE = Money.ofCents(1500);
    private static final LocalTime ENTRY_START = LocalTime.of(6, 0);
    private static final LocalTime ENTRY_END = LocalTime.of(9, 0);
    private static final LocalTime EXIT_START = LocalTime.of(15, 30);
    private static final LocalTime EXIT_END = LocalTime.of(19, 0);

    private final ZoneId zoneId;
    private final HolidayCalendar holidayCalendar;

    public EarlyBirdPolicy(ZoneId zoneId, HolidayCalendar holidayCalendar) {
        this.zoneId = Objects.requireNonNull(zoneId, "zoneId");
        this.holidayCalendar = Objects.requireNonNull(holidayCalendar, "holidayCalendar");
    }

    @Override
    public PolicyType policyType() {
        return PolicyType.EARLY_BIRD;
    }

    @Override
    public Optional<PolicyQuote> quote(ParkingSession session) {
        Objects.requireNonNull(session, "session");
        var entry = session.interval().entryAt().atZone(zoneId);
        var exit = session.interval().exitAt().atZone(zoneId);
        LocalDate date = entry.toLocalDate();
        if (!date.equals(exit.toLocalDate())
                || !PricingTime.isEligibleWeekday(date, holidayCalendar)
                || entry.toLocalTime().isBefore(ENTRY_START)
                || !entry.toLocalTime().isBefore(ENTRY_END)
                || exit.toLocalTime().isBefore(EXIT_START)
                || !exit.toLocalTime().isBefore(EXIT_END)
                || session.interval().duration().compareTo(Duration.ofHours(24)) > 0) {
            return Optional.empty();
        }

        Money total = applyAdjustments(BASE_FEE, session);
        return Optional.of(new PolicyQuote(PolicyType.EARLY_BIRD, total, List.of()));
    }

    private static Money applyAdjustments(Money baseFee, ParkingSession session) {
        BigDecimal discount = session.loyaltyTier()
                .map(LoyaltyTier::discountRate)
                .orElse(BigDecimal.ZERO);
        BigDecimal multiplier = session.vehicleClass().multiplier()
                .multiply(BigDecimal.ONE.subtract(discount));
        return Money.ofDollars(baseFee.asBigDecimal().multiply(multiplier));
    }
}
