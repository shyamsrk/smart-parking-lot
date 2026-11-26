package com.smartparkinglot.domain;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ParkingSpotTest {
    @Test
    void reservesOnlyCompatibleAvailableSpots() {
        ParkingSpot spot = new ParkingSpot("C-1", SpotType.COMPACT);

        spot.reserveFor("T-1", VehicleClass.CAR);

        assertFalse(spot.isAvailable());
        assertEquals("T-1", spot.occupiedByTicketId().orElseThrow());
    }

    @Test
    void failedReservationDoesNotChangeOccupancy() {
        ParkingSpot spot = new ParkingSpot("C-1", SpotType.COMPACT);

        assertThrows(
                IllegalArgumentException.class,
                () -> spot.reserveFor("T-1", VehicleClass.BUS));
        assertTrue(spot.isAvailable());

        spot.reserveFor("T-1", VehicleClass.CAR);
        assertThrows(
                IllegalStateException.class,
                () -> spot.reserveFor("T-2", VehicleClass.CAR));
        assertEquals("T-1", spot.occupiedByTicketId().orElseThrow());
    }

    @Test
    void releasesOnlyForOwningTicket() {
        ParkingSpot spot = new ParkingSpot("L-1", SpotType.LARGE);
        spot.reserveFor("T-1", VehicleClass.BUS);

        assertThrows(IllegalStateException.class, () -> spot.release("T-2"));
        assertFalse(spot.isAvailable());

        spot.release("T-1");
        assertTrue(spot.isAvailable());
    }
}
