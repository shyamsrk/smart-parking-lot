package com.smartparkinglot.domain;

import java.math.BigDecimal;

public enum VehicleClass {
    MOTORCYCLE("0.8"),
    CAR("1.0"),
    BUS("2.0");

    private final BigDecimal multiplier;

    VehicleClass(String multiplier) {
        this.multiplier = new BigDecimal(multiplier);
    }

    public BigDecimal multiplier() {
        return multiplier;
    }
}
