package com.afcgym.platform.service.dto;

import java.math.BigDecimal;

public record CreateProductCommand(
        String code,
        String name,
        Long categoryId,
        Long supplierId,
        BigDecimal price,
        BigDecimal cost,
        Integer stock,
        Integer minStock
) {}
