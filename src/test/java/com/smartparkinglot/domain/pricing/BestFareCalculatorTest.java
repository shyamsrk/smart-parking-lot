package com.smartparkinglot.domain.pricing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.smartparkinglot.domain.LoyaltyTier;
import com.smartparkinglot.domain.Money;
import com.smartparkinglot.domain.ParkingInterval;
import com.smartparkinglot.domain.VehicleClass;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class BestFareCalculatorTest {
    private static final ZoneId ZONE = ZoneId.of("America/Los_Angeles");
    private static final HolidayCalendar NO_HOLIDAYS = date -> false;

    @Test
    void evaluatesEveryPolicyAndReportsApplicableAndInapplicableResults() {
        List<PolicyType> evaluated = new ArrayList<>();
        BestFareCalculator calculator = calculatorWithTrackingPolicies(evaluated);

        FareComparison comparison = calculator.calculate(session(
                Instant.parse("2026-11-23T14:30:00Z"),
                Instant.parse("2026-11-23T16:30:00Z"),
                VehicleClass.CAR,
                Optional.empty()));

        assertEquals(
                List.of(PolicyType.STANDARD_HOURLY, PolicyType.EARLY_BIRD, PolicyType.NIGHT_OWL),
                evaluated);
        assertEquals(3, comparison.policyEvaluations().size());
        assertTrue(comparison.policyEvaluations().get(0).quote().isPresent());
        assertTrue(comparison.policyEvaluations().get(1).quote().isEmpty());
        assertTrue(comparison.policyEvaluations().get(2).quote().isEmpty());
    }

    @Test
    void selectsStandardHourlyDocumentedFloatingBlockExample() {
        FareComparison comparison = createCalculator().calculate(session(
                Instant.parse("2026-11-23T14:30:00Z"),
                Instant.parse("2026-11-23T16:30:00Z"),
                VehicleClass.CAR,
                Optional.empty()));

        assertEquals(PolicyType.STANDARD_HOURLY, comparison.winningQuote().policyType());
        assertEquals(Money.ofCents(1200), comparison.winningQuote().total());
        assertEquals(2, comparison.winningQuote().hourlyBreakdown().size());
        assertFalse(comparison.tieBreakApplied());
    }

    @Test
    void selectsGoldEarlyBirdOverHourlyAndReportsBothTotals() {
        FareComparison comparison = createCalculator().calculate(session(
                Instant.parse("2026-11-23T14:30:00Z"),
                Instant.parse("2026-11-24T00:00:00Z"),
                VehicleClass.CAR,
                Optional.of(LoyaltyTier.GOLD)));

        assertEquals(PolicyType.EARLY_BIRD, comparison.winningQuote().policyType());
        assertEquals(Money.ofCents(1200), comparison.winningQuote().total());
        assertEquals(
                Money.ofCents(3100),
                comparison.policyEvaluations().get(0).quote().orElseThrow().total());
        assertEquals(
                Money.ofCents(1200),
                comparison.policyEvaluations().get(1).quote().orElseThrow().total());
    }

    @Test
    void appliesTiePrecedenceEarlyBirdThenNightOwlThenStandardHourly() {
        ParkingSession session = session(
                Instant.parse("2026-11-23T14:30:00Z"),
                Instant.parse("2026-11-23T16:30:00Z"),
                VehicleClass.CAR,
                Optional.empty());
        BestFareCalculator allTie = calculatorWithAmounts(
                Money.ofCents(1000), Money.ofCents(1000), Money.ofCents(1000));
        BestFareCalculator nightAndHourlyTie = calculatorWithAmounts(
                Money.ofCents(1000), null, Money.ofCents(1000));

        FareComparison first = allTie.calculate(session);
        FareComparison second = nightAndHourlyTie.calculate(session);

        assertEquals(PolicyType.EARLY_BIRD, first.winningQuote().policyType());
        assertTrue(first.tieBreakApplied());
        assertEquals(PolicyType.NIGHT_OWL, second.winningQuote().policyType());
        assertTrue(second.tieBreakApplied());
    }

    @Test
    void excludesSpecialsOverTwentyFourHoursEvenIfPolicyReturnsQuote() {
        BestFareCalculator calculator = calculatorWithAmounts(
                Money.ofCents(5000), Money.ofCents(100), Money.ofCents(100));
        FareComparison comparison = calculator.calculate(session(
                Instant.parse("2026-11-23T14:00:00Z"),
                Instant.parse("2026-11-24T15:00:00Z"),
                VehicleClass.CAR,
                Optional.empty()));

        assertTrue(comparison.policyEvaluations().get(0).quote().isPresent());
        assertTrue(comparison.policyEvaluations().get(1).quote().isEmpty());
        assertTrue(comparison.policyEvaluations().get(2).quote().isEmpty());
        assertEquals(PolicyType.STANDARD_HOURLY, comparison.winningQuote().policyType());
    }

    private static BestFareCalculator createCalculator() {
        return new BestFareCalculator(List.of(
                new StandardHourlyPolicy(ZONE, NO_HOLIDAYS),
                new EarlyBirdPolicy(ZONE, NO_HOLIDAYS),
                new NightOwlPolicy(ZONE, NO_HOLIDAYS)));
    }

    private static BestFareCalculator calculatorWithTrackingPolicies(
            List<PolicyType> evaluated) {
        return new BestFareCalculator(List.of(
                policy(PolicyType.STANDARD_HOURLY, Money.ofCents(1200), evaluated),
                policy(PolicyType.EARLY_BIRD, null, evaluated),
                policy(PolicyType.NIGHT_OWL, null, evaluated)));
    }

    private static BestFareCalculator calculatorWithAmounts(
            Money standard, Money earlyBird, Money nightOwl) {
        return new BestFareCalculator(List.of(
                policy(PolicyType.STANDARD_HOURLY, standard, new ArrayList<>()),
                policy(PolicyType.EARLY_BIRD, earlyBird, new ArrayList<>()),
                policy(PolicyType.NIGHT_OWL, nightOwl, new ArrayList<>())));
    }

    private static PricingPolicy policy(
            PolicyType type, Money amount, List<PolicyType> evaluated) {
        return new PricingPolicy() {
            @Override
            public PolicyType policyType() {
                return type;
            }

            @Override
            public Optional<PolicyQuote> quote(ParkingSession session) {
                evaluated.add(type);
                if (amount == null) {
                    return Optional.empty();
                }
                return Optional.of(new PolicyQuote(type, amount, List.of()));
            }
        };
    }

    private static ParkingSession session(
            Instant entry, Instant exit, VehicleClass vehicleClass,
            Optional<LoyaltyTier> loyaltyTier) {
        return new ParkingSession(
                new ParkingInterval(entry, exit), vehicleClass, loyaltyTier);
    }
}
