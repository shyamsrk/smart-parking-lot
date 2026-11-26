package com.smartparkinglot.domain.pricing;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class UsFederalHolidayCalendarTest {
    @Test
    void usesOfficialObservedDatesAndResolvesDatesByCalendarYear() {
        UsFederalHolidayCalendar calendar =
                UsFederalHolidayCalendar.forYears(2021, 2022);

        assertTrue(calendar.isHoliday(LocalDate.of(2021, 7, 5)));
        assertTrue(calendar.isHoliday(LocalDate.of(2021, 12, 31)));
        assertTrue(calendar.isHoliday(LocalDate.of(2022, 1, 17)));
        assertFalse(calendar.isHoliday(LocalDate.of(2021, 7, 4)));
        assertFalse(calendar.isHoliday(LocalDate.of(2022, 1, 1)));
    }

    @Test
    void defaultsToYearFromInjectedClock() {
        Clock clock = Clock.fixed(
                Instant.parse("2030-07-01T00:00:00Z"), ZoneOffset.UTC);
        UsFederalHolidayCalendar calendar =
                UsFederalHolidayCalendar.forCurrentYear(clock);

        assertTrue(calendar.isHoliday(LocalDate.of(2030, 1, 1)));
        assertFalse(calendar.isHoliday(LocalDate.of(2029, 1, 1)));
    }
}
