package com.afcgym.platform.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import java.util.Set;
import org.junit.jupiter.api.Test;

class AttendanceStreakCalculatorTest {

    @Test
    void givenNoAttendance_whenCounting_thenReturnZero() {
        long streak = AttendanceStreakCalculator.countConsecutiveBusinessDays(
                Set.of(), LocalDate.of(2026, 11, 17));

        assertEquals(0, streak);
    }

    @Test
    void givenFiveConsecutiveBusinessDays_whenCounting_thenReturnFive() {
        LocalDate tuesday = LocalDate.of(2026, 11, 17);
        Set<LocalDate> attendance = Set.of(
                LocalDate.of(2026, 11, 11),
                LocalDate.of(2026, 11, 12),
                LocalDate.of(2026, 11, 13),
                LocalDate.of(2026, 11, 16),
                LocalDate.of(2026, 11, 17));

        long streak = AttendanceStreakCalculator.countConsecutiveBusinessDays(attendance, tuesday);

        assertEquals(5, streak);
    }

    @Test
    void givenAMissingBusinessDay_whenCounting_thenStopTheStreakThere() {
        LocalDate tuesday = LocalDate.of(2026, 11, 17);
        Set<LocalDate> attendance = Set.of(
                LocalDate.of(2026, 11, 11),
                LocalDate.of(2026, 11, 12),
                LocalDate.of(2026, 11, 16),
                LocalDate.of(2026, 11, 17));

        long streak = AttendanceStreakCalculator.countConsecutiveBusinessDays(attendance, tuesday);

        assertEquals(2, streak);
    }

    @Test
    void givenNoAttendanceToday_whenCountingFromAWeekend_thenCountBackFromFriday() {
        LocalDate sunday = LocalDate.of(2026, 11, 15);
        Set<LocalDate> attendance = Set.of(
                LocalDate.of(2026, 11, 13),
                LocalDate.of(2026, 11, 12));

        long streak = AttendanceStreakCalculator.countConsecutiveBusinessDays(attendance, sunday);

        assertEquals(2, streak);
    }

    @Test
    void givenNoCheckInYetToday_whenCounting_thenStillCountYesterdaysStreak() {
        LocalDate tuesday = LocalDate.of(2026, 11, 17);
        Set<LocalDate> attendance = Set.of(
                LocalDate.of(2026, 11, 16));

        long streak = AttendanceStreakCalculator.countConsecutiveBusinessDays(attendance, tuesday);

        assertEquals(1, streak);
    }
}
