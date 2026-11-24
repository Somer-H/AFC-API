package com.afcgym.platform.utils;

import com.afcgym.platform.persistance.entities.enums.MembershipType;

public final class PaymentReferenceFormatter {

    private static final String REFERENCE_FORMAT = "PAG-%06d";

    private PaymentReferenceFormatter() {
    }

    public static String reference(Long paymentId) {
        return paymentId == null ? null : String.format(REFERENCE_FORMAT, paymentId);
    }

    public static String membershipName(MembershipType membershipType) {
        return switch (membershipType) {
            case WEEKLY -> "semanal";
            case BIWEEKLY -> "quincenal";
            case MONTHLY -> "mensual";
            case QUARTERLY -> "trimestral";
            case YEARLY -> "anual";
        };
    }
}
