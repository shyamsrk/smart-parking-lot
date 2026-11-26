package com.smartparkinglot.domain.pricing;

import com.smartparkinglot.domain.Money;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class BestFareCalculator {
    private static final List<PolicyType> TIE_BREAK_ORDER =
            List.of(PolicyType.EARLY_BIRD, PolicyType.NIGHT_OWL, PolicyType.STANDARD_HOURLY);

    private final List<PricingPolicy> policies;

    public BestFareCalculator(List<PricingPolicy> policies) {
        this.policies = List.copyOf(Objects.requireNonNull(policies, "policies"));
        EnumSet<PolicyType> policyTypes = EnumSet.noneOf(PolicyType.class);
        for (PricingPolicy policy : this.policies) {
            Objects.requireNonNull(policy, "policy");
            if (!policyTypes.add(Objects.requireNonNull(policy.policyType(), "policyType"))) {
                throw new IllegalArgumentException("Policy type configured more than once");
            }
        }
        if (!policyTypes.equals(EnumSet.allOf(PolicyType.class))) {
            throw new IllegalArgumentException("Exactly one of each pricing policy is required");
        }
    }

    public FareComparison calculate(ParkingSession session) {
        Objects.requireNonNull(session, "session");
        boolean longerThan24Hours =
                session.interval().duration().compareTo(Duration.ofHours(24)) > 0;
        List<PolicyEvaluation> evaluations = new ArrayList<>(policies.size());

        for (PricingPolicy policy : policies) {
            Optional<PolicyQuote> quote = Objects.requireNonNull(
                    policy.quote(session), "policy quote");
            if (longerThan24Hours && policy.policyType() != PolicyType.STANDARD_HOURLY) {
                quote = Optional.empty();
            }
            evaluations.add(new PolicyEvaluation(policy.policyType(), quote));
        }

        List<PolicyQuote> applicableQuotes = evaluations.stream()
                .flatMap(evaluation -> evaluation.quote().stream())
                .toList();
        if (applicableQuotes.isEmpty()) {
            throw new IllegalStateException("No pricing policy returned an applicable quote");
        }

        Money lowestTotal = applicableQuotes.stream()
                .map(PolicyQuote::total)
                .min(Comparator.naturalOrder())
                .orElseThrow();
        List<PolicyQuote> lowestQuotes = applicableQuotes.stream()
                .filter(quote -> quote.total().compareTo(lowestTotal) == 0)
                .toList();
        PolicyQuote winner = lowestQuotes.stream()
                .min(Comparator.comparingInt(
                        quote -> TIE_BREAK_ORDER.indexOf(quote.policyType())))
                .orElseThrow();

        return new FareComparison(evaluations, winner, lowestQuotes.size() > 1);
    }
}
