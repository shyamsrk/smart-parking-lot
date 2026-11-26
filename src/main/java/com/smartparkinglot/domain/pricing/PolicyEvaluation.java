package com.smartparkinglot.domain.pricing;

import java.util.Objects;
import java.util.Optional;

public record PolicyEvaluation(PolicyType policyType, Optional<PolicyQuote> quote) {
    public PolicyEvaluation {
        Objects.requireNonNull(policyType, "policyType");
        Objects.requireNonNull(quote, "quote");
        quote.ifPresent(policyQuote -> {
            if (policyQuote.policyType() != policyType) {
                throw new IllegalArgumentException("Quote policy type does not match evaluation");
            }
        });
    }

    public boolean isApplicable() {
        return quote.isPresent();
    }
}
