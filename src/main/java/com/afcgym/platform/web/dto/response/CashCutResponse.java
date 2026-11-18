package com.afcgym.platform.web.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import lombok.Builder;

@Builder
public record CashCutResponse(
        String period,
        String category,
        LocalDate from,
        LocalDate to,
        BigDecimal membershipIncome,
        BigDecimal storeIncome,
        BigDecimal totalIncome,
        int membershipPayments,
        int storeSales,
        List<PaymentMethodBreakdownResponse> paymentMethods
) {}
