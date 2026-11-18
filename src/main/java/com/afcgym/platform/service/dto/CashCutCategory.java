package com.afcgym.platform.service.dto;

public enum CashCutCategory {
    MEMBERSHIPS,
    STORE,
    ALL;

    public boolean includesMemberships() {
        return this != STORE;
    }

    public boolean includesStore() {
        return this != MEMBERSHIPS;
    }
}
