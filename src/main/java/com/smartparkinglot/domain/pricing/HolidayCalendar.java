package com.smartparkinglot.domain.pricing;

import java.time.LocalDate;

@FunctionalInterface
public interface HolidayCalendar {
    boolean isHoliday(LocalDate date);
}
