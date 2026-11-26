package com.smartparkinglot.domain.pricing;

import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public record FareComparison(
        List<PolicyEvaluation> policyEvaluations,
        PolicyQuote winningQuote,
        boolean tieBreakApplied) {
    public FareComparison {
        policyEvaluations = List.copyOf(
                Objects.requireNonNull(policyEvaluations, "policyEvaluations"));
        Objects.requireNonNull(winningQuote, "winningQuote");

        Set<PolicyType> evaluatedTypes = EnumSet.noneOf(PolicyType.class);
        for (PolicyEvaluation evaluation : policyEvaluations) {
            Objects.requireNonNull(evaluation, "policyEvaluation");
            if (!evaluatedTypes.add(evaluation.policyType())) {
                throw new IllegalArgumentException("Policy type evaluated more than once");
            }
        }
        if (!evaluatedTypes.equals(EnumSet.allOf(PolicyType.class))) {
            throw new IllegalArgumentException("All pricing policies must be evaluated");
        }
        boolean winnerWasEvaluated = policyEvaluations.stream()
                .anyMatch(evaluation -> evaluation.quote()
                        .filter(winningQuote::equals)
                        .isPresent());
        if (!winnerWasEvaluated) {
            throw new IllegalArgumentException("Winning quote must be an evaluated quote");
        }
    }
}
