package com.smartparkinglot.domain.pricing;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Objects;

final class PricingTime {
    private PricingTime() {
    }

    static boolean isEligibleWeekday(
            LocalDate date, HolidayCalendar holidayCalendar) {
        Objects.requireNonNull(date, "date");
        return date.getDayOfWeek() != DayOfWeek.SATURDAY
                && date.getDayOfWeek() != DayOfWeek.SUNDAY
                && !holidayCalendar.isHoliday(date);
    }
}
