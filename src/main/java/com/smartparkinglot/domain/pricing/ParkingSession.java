package com.smartparkinglot.domain.pricing;

import com.smartparkinglot.domain.LoyaltyTier;
import com.smartparkinglot.domain.ParkingInterval;
import com.smartparkinglot.domain.VehicleClass;
import java.util.Objects;
import java.util.Optional;

public record ParkingSession(
        ParkingInterval interval,
        VehicleClass vehicleClass,
        Optional<LoyaltyTier> loyaltyTier) {
    public ParkingSession {
        Objects.requireNonNull(interval, "interval");
        Objects.requireNonNull(vehicleClass, "vehicleClass");
        Objects.requireNonNull(loyaltyTier, "loyaltyTier");
    }
}
