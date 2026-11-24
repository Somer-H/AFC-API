package com.afcgym.platform.persistance.entities.enums;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public enum MemberStatus {
    ACTIVE,
    EXPIRED,
    TO_PURGE;

    public static final long TO_PURGE_THRESHOLD_DAYS = 60;

    public static MemberStatus fromDueDate(LocalDate dueDate) {
        long daysLate = ChronoUnit.DAYS.between(dueDate, LocalDate.now());
        if (daysLate <= 0) {
            return ACTIVE;
        }
        if (daysLate <= TO_PURGE_THRESHOLD_DAYS) {
            return EXPIRED;
        }
        return TO_PURGE;
    }
}
