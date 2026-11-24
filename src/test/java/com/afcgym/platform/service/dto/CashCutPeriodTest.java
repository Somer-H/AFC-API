package com.afcgym.platform.service.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class CashCutPeriodTest {

    @Test
    void givenADate_whenDaily_thenTheRangeIsThatSameDay() {
        LocalDate wednesday = LocalDate.of(2026, 11, 18);

        assertEquals(wednesday, CashCutPeriod.DAILY.startOf(wednesday));
        assertEquals(wednesday, CashCutPeriod.DAILY.endOf(wednesday));
    }

    @Test
    void givenAWednesday_whenWeekly_thenTheRangeGoesFromMondayToSunday() {
        LocalDate wednesday = LocalDate.of(2026, 11, 18);

        assertEquals(LocalDate.of(2026, 11, 16), CashCutPeriod.WEEKLY.startOf(wednesday));
        assertEquals(LocalDate.of(2026, 11, 22), CashCutPeriod.WEEKLY.endOf(wednesday));
    }

    @Test
    void givenASunday_whenWeekly_thenItBelongsToTheWeekThatStartedOnTheMondayBefore() {
        LocalDate sunday = LocalDate.of(2026, 11, 22);

        assertEquals(LocalDate.of(2026, 11, 16), CashCutPeriod.WEEKLY.startOf(sunday));
        assertEquals(sunday, CashCutPeriod.WEEKLY.endOf(sunday));
    }

    @Test
    void givenADateInALeapFebruary_whenMonthly_thenTheRangeCoversTheWholeMonth() {
        LocalDate reference = LocalDate.of(2028, 2, 10);

        assertEquals(LocalDate.of(2028, 2, 1), CashCutPeriod.MONTHLY.startOf(reference));
        assertEquals(LocalDate.of(2028, 2, 29), CashCutPeriod.MONTHLY.endOf(reference));
    }
}
