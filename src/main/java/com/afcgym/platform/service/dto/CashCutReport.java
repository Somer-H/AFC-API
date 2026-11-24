package com.afcgym.platform.service.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record CashCutReport(
        CashCutPeriod period,
        CashCutCategory category,
        LocalDate from,
        LocalDate to,
        BigDecimal membershipIncome,
        BigDecimal storeIncome,
        BigDecimal totalIncome,
        int membershipPayments,
        int storeSales,
        List<PaymentMethodBreakdown> paymentMethods
) {}
