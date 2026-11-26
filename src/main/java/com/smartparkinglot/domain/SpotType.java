package com.smartparkinglot.domain;

import java.util.Objects;

public enum SpotType {
    COMPACT,
    LARGE;

    public boolean supports(VehicleClass vehicleClass) {
        Objects.requireNonNull(vehicleClass, "vehicleClass");
        return switch (this) {
            case COMPACT -> vehicleClass == VehicleClass.MOTORCYCLE
                    || vehicleClass == VehicleClass.CAR;
            case LARGE -> vehicleClass == VehicleClass.BUS;
        };
    }
}
