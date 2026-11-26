package com.smartparkinglot.domain.pricing;

import com.smartparkinglot.domain.Money;
import java.util.List;
import java.util.Objects;

public record PolicyQuote(
        PolicyType policyType, Money total, List<HourlySegment> hourlyBreakdown) {
    public PolicyQuote {
        Objects.requireNonNull(policyType, "policyType");
        Objects.requireNonNull(total, "total");
        hourlyBreakdown = List.copyOf(
                Objects.requireNonNull(hourlyBreakdown, "hourlyBreakdown"));
    }
}
