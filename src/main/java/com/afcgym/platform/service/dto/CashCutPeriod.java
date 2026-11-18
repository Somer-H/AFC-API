package com.afcgym.platform.service.dto;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

public enum CashCutPeriod {
    DAILY,
    WEEKLY,
    MONTHLY;

    public LocalDate startOf(LocalDate reference) {
        return switch (this) {
            case DAILY -> reference;
            case WEEKLY -> reference.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            case MONTHLY -> reference.withDayOfMonth(1);
        };
    }

    public LocalDate endOf(LocalDate reference) {
        return switch (this) {
            case DAILY -> reference;
            case WEEKLY -> reference.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
            case MONTHLY -> reference.with(TemporalAdjusters.lastDayOfMonth());
        };
    }
}
