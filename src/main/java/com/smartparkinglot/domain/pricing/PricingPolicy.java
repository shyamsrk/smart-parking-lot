package com.smartparkinglot.domain.pricing;

import java.util.Optional;

public interface PricingPolicy {
    PolicyType policyType();

    Optional<PolicyQuote> quote(ParkingSession session);
}
