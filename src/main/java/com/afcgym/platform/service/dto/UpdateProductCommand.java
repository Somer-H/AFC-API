package com.afcgym.platform.service.dto;

import java.math.BigDecimal;

public record UpdateProductCommand(
        String name,
        Long categoryId,
        Long supplierId,
        BigDecimal price,
        BigDecimal cost,
        Integer minStock
) {}
