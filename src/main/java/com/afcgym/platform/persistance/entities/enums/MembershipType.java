package com.afcgym.platform.persistance.entities.enums;

import java.math.BigDecimal;

public enum MembershipType {
    WEEKLY(7, new BigDecimal("150.00")),
    BIWEEKLY(14, new BigDecimal("280.00")),
    MONTHLY(30, new BigDecimal("500.00")),
    QUARTERLY(90, new BigDecimal("1350.00")),
    YEARLY(365, new BigDecimal("4800.00"));

    private final int durationDays;
    private final BigDecimal basePrice;

    MembershipType(int durationDays, BigDecimal basePrice) {
        this.durationDays = durationDays;
        this.basePrice = basePrice;
    }

    public int getDurationDays() {
        return durationDays;
    }

    public BigDecimal getBasePrice() {
        return basePrice;
    }
}
