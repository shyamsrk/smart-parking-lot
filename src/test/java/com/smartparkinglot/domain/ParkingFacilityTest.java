package com.smartparkinglot.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.Test;

class ParkingFacilityTest {
    @Test
    void preservesConfiguredLevelAndSpotOrderAndZone() {
        ParkingSpot first = new ParkingSpot("C-1", SpotType.COMPACT);
        ParkingSpot second = new ParkingSpot("L-1", SpotType.LARGE);
        ParkingLevel level1 = new ParkingLevel("1", List.of(first));
        ParkingLevel level2 = new ParkingLevel("2", List.of(second));
        ParkingFacility facility =
                new ParkingFacility("MAIN", ZoneId.of("America/Los_Angeles"),
                        List.of(level1, level2));

        assertEquals(ZoneId.of("America/Los_Angeles"), facility.timeZone());
        assertEquals(List.of(level1, level2), facility.levels());
        assertEquals(List.of(first, second), facility.spots());
    }

    @Test
    void rejectsDuplicateSpotIdentifiersAcrossLevels() {
        ParkingLevel level1 =
                new ParkingLevel("1", List.of(new ParkingSpot("S-1", SpotType.COMPACT)));
        ParkingLevel level2 =
                new ParkingLevel("2", List.of(new ParkingSpot("S-1", SpotType.LARGE)));

        assertThrows(
                IllegalArgumentException.class,
                () -> new ParkingFacility("MAIN", ZoneId.of("UTC"),
                        List.of(level1, level2)));
    }
}
