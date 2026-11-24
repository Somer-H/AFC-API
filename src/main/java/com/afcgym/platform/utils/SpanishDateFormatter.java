package com.afcgym.platform.utils;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Locale;

public final class SpanishDateFormatter {

    private static final Locale SPANISH = Locale.of("es", "MX");
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private SpanishDateFormatter() {
    }

    public static String dayName(DayOfWeek dayOfWeek) {
        return dayOfWeek.getDisplayName(TextStyle.FULL, SPANISH);
    }

    public static String date(LocalDate date) {
        return date.format(DATE_FORMAT);
    }
}
