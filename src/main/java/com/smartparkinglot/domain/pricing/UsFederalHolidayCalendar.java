package com.smartparkinglot.domain.pricing;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Month;
import java.time.Year;
import java.time.temporal.TemporalAdjusters;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public final class UsFederalHolidayCalendar implements HolidayCalendar {
    private final Set<LocalDate> observedDates;

    private UsFederalHolidayCalendar(Set<LocalDate> observedDates) {
        this.observedDates = Set.copyOf(observedDates);
    }

    public static UsFederalHolidayCalendar forCurrentYear(Clock clock) {
        Objects.requireNonNull(clock, "clock");
        return forYears(Year.now(clock).getValue());
    }

    public static UsFederalHolidayCalendar forYears(int... years) {
        Objects.requireNonNull(years, "years");
        return forYears(Arrays.stream(years).boxed().toList(), Set.of());
    }

    public static UsFederalHolidayCalendar forYears(
            Collection<Integer> years, Collection<LocalDate> additionalObservedDates) {
        Objects.requireNonNull(years, "years");
        Objects.requireNonNull(additionalObservedDates, "additionalObservedDates");
        if (years.isEmpty()) {
            throw new IllegalArgumentException("At least one calendar year is required");
        }

        Set<LocalDate> dates = new HashSet<>();
        for (Integer year : years) {
            Objects.requireNonNull(year, "year");
            dates.addAll(observedDatesInCalendarYear(year));
        }
        dates.addAll(additionalObservedDates);
        return new UsFederalHolidayCalendar(dates);
    }

    public static UsFederalHolidayCalendar ofObservedDates(
            Collection<LocalDate> observedDates) {
        Objects.requireNonNull(observedDates, "observedDates");
        return new UsFederalHolidayCalendar(Set.copyOf(observedDates));
    }

    @Override
    public boolean isHoliday(LocalDate date) {
        return observedDates.contains(Objects.requireNonNull(date, "date"));
    }

    private static Set<LocalDate> observedDatesInCalendarYear(int year) {
        Set<LocalDate> observed = new HashSet<>();
        for (LocalDate holiday : federalHolidays(year)) {
            LocalDate observedDate = observe(holiday);
            if (observedDate.getYear() == year) {
                observed.add(observedDate);
            }
        }

        LocalDate nextNewYearObserved = observe(LocalDate.of(year + 1, Month.JANUARY, 1));
        if (nextNewYearObserved.getYear() == year) {
            observed.add(nextNewYearObserved);
        }
        return observed;
    }

    private static Set<LocalDate> federalHolidays(int year) {
        Set<LocalDate> holidays = new HashSet<>();
        holidays.add(LocalDate.of(year, Month.JANUARY, 1));
        holidays.add(nthWeekday(year, Month.JANUARY, DayOfWeek.MONDAY, 3));
        holidays.add(nthWeekday(year, Month.FEBRUARY, DayOfWeek.MONDAY, 3));
        holidays.add(LocalDate.of(year, Month.MAY, 1)
                .with(TemporalAdjusters.lastInMonth(DayOfWeek.MONDAY)));
        if (year >= 2021) {
            holidays.add(LocalDate.of(year, Month.JUNE, 19));
        }
        holidays.add(LocalDate.of(year, Month.JULY, 4));
        holidays.add(nthWeekday(year, Month.SEPTEMBER, DayOfWeek.MONDAY, 1));
        holidays.add(nthWeekday(year, Month.OCTOBER, DayOfWeek.MONDAY, 2));
        holidays.add(LocalDate.of(year, Month.NOVEMBER, 11));
        holidays.add(nthWeekday(year, Month.NOVEMBER, DayOfWeek.THURSDAY, 4));
        holidays.add(LocalDate.of(year, Month.DECEMBER, 25));
        return holidays;
    }

    private static LocalDate nthWeekday(
            int year, Month month, DayOfWeek dayOfWeek, int ordinal) {
        return LocalDate.of(year, month, 1)
                .with(TemporalAdjusters.dayOfWeekInMonth(ordinal, dayOfWeek));
    }

    private static LocalDate observe(LocalDate holiday) {
        return switch (holiday.getDayOfWeek()) {
            case SATURDAY -> holiday.minusDays(1);
            case SUNDAY -> holiday.plusDays(1);
            default -> holiday;
        };
    }
}
