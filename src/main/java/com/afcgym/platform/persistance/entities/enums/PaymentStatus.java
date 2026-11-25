package com.afcgym.platform.persistance.entities.enums;

import java.util.Arrays;
import java.util.List;

public enum PaymentStatus {
    PENDING,
    AWAITING_GATEWAY,
    GATEWAY_REJECTED,
    PENDING_BANK_VALIDATION,
    PENDING_APPOINTMENT,
    APPOINTMENT_CONFIRMED,
    APPOINTMENT_CANCELLED,
    APPROVED,
    REJECTED,
    CANCELLED;

    public boolean isOpen() {
        return switch (this) {
            case PENDING, AWAITING_GATEWAY, PENDING_BANK_VALIDATION, PENDING_APPOINTMENT, APPOINTMENT_CONFIRMED,
                 APPOINTMENT_CANCELLED -> true;
            case GATEWAY_REJECTED, APPROVED, REJECTED, CANCELLED -> false;
        };
    }

    public boolean isWaitingForAppointment() {
        return this == PENDING_APPOINTMENT || this == APPOINTMENT_CONFIRMED || this == APPOINTMENT_CANCELLED;
    }

    public static List<PaymentStatus> openStatuses() {
        return Arrays.stream(values()).filter(PaymentStatus::isOpen).toList();
    }
}
