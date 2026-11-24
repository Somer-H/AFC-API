package com.afcgym.platform.web.dto.response;

import java.math.BigDecimal;
import lombok.Builder;

@Builder
public record PaymentMethodBreakdownResponse(
        String method,
        BigDecimal amount,
        BigDecimal percentage,
        int transactions
) {}
