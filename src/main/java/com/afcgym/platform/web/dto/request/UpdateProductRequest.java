package com.afcgym.platform.web.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

public record UpdateProductRequest(
        @NotBlank String name,
        Long categoryId,
        Long supplierId,
        @NotNull @Positive BigDecimal price,
        BigDecimal cost,
        @NotNull @PositiveOrZero Integer minStock
) {}
