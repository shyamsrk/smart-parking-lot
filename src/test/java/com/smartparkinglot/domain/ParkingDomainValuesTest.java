package com.smartparkinglot.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class ParkingDomainValuesTest {
    @Test
    void definesVehicleMultipliers() {
        assertEquals(new BigDecimal("0.8"), VehicleClass.MOTORCYCLE.multiplier());
        assertEquals(new BigDecimal("1.0"), VehicleClass.CAR.multiplier());
        assertEquals(new BigDecimal("2.0"), VehicleClass.BUS.multiplier());
    }

    @Test
    void definesLoyaltyDiscountRates() {
        assertEquals(new BigDecimal("0.10"), LoyaltyTier.SILVER.discountRate());
        assertEquals(new BigDecimal("0.20"), LoyaltyTier.GOLD.discountRate());
        assertEquals(new BigDecimal("0.30"), LoyaltyTier.PLATINUM.discountRate());
    }

    @Test
    void compactAndLargeSpotsHaveExpectedCompatibility() {
        assertTrue(SpotType.COMPACT.supports(VehicleClass.MOTORCYCLE));
        assertTrue(SpotType.COMPACT.supports(VehicleClass.CAR));
        assertFalse(SpotType.COMPACT.supports(VehicleClass.BUS));
        assertTrue(SpotType.LARGE.supports(VehicleClass.BUS));
        assertFalse(SpotType.LARGE.supports(VehicleClass.CAR));
    }
}
