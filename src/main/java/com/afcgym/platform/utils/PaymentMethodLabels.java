package com.afcgym.platform.utils;

import com.afcgym.platform.persistance.entities.enums.PaymentMethod;

public final class PaymentMethodLabels {

    private PaymentMethodLabels() {
    }

    public static String spanish(PaymentMethod method) {
        return switch (method) {
            case CASH -> "Efectivo";
            case CARD -> "Tarjeta";
            case SPEI -> "Transferencia (SPEI)";
        };
    }
}
