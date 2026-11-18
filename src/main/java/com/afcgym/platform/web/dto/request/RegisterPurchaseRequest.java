package com.afcgym.platform.web.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record RegisterPurchaseRequest(
        @NotNull @Positive Integer quantity,
        BigDecimal unitCost
) {}
