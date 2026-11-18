package com.afcgym.platform.web.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record SaleLineRequest(
        @NotNull Long productId,
        @NotNull @Positive Integer quantity
) {}
