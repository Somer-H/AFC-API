package com.afcgym.platform.persistance.entities.enums;

import java.time.LocalTime;

public enum Shift {
    MORNING,
    EVENING;

    private static final LocalTime NOON = LocalTime.NOON;

    public static Shift fromStartTime(LocalTime startTime) {
        return startTime.isBefore(NOON) ? MORNING : EVENING;
    }
}
