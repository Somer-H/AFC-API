package com.afcgym.platform.utils;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Set;

public final class AttendanceStreakCalculator {

    private AttendanceStreakCalculator() {
    }

    public static long countConsecutiveBusinessDays(Set<LocalDate> attendanceDates, LocalDate referenceDate) {
        LocalDate cursor = lastBusinessDayOnOrBefore(referenceDate);
        if (!attendanceDates.contains(cursor)) {
            cursor = previousBusinessDay(cursor);
        }
        long streak = 0;
        while (attendanceDates.contains(cursor)) {
            streak++;
            cursor = previousBusinessDay(cursor);
        }
        return streak;
    }

    private static LocalDate lastBusinessDayOnOrBefore(LocalDate date) {
        LocalDate result = date;
        while (isWeekend(result)) {
            result = result.minusDays(1);
        }
        return result;
    }

    private static LocalDate previousBusinessDay(LocalDate date) {
        LocalDate result = date.minusDays(1);
        while (isWeekend(result)) {
            result = result.minusDays(1);
        }
        return result;
    }

    private static boolean isWeekend(LocalDate date) {
        return date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY;
    }
}
