package com.afcgym.platform.web.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record CreateProductRequest(
        @NotBlank String code,
        @NotBlank String name,
        Long categoryId,
        Long supplierId,
        @NotNull @Positive BigDecimal price,
        BigDecimal cost,
        Integer stock,
        Integer minStock
) {}
