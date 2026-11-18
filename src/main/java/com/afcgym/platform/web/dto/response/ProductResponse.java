package com.afcgym.platform.web.dto.response;

import java.math.BigDecimal;
import lombok.Builder;

@Builder
public record ProductResponse(
        Long id,
        String code,
        String name,
        Long categoryId,
        Long supplierId,
        BigDecimal price,
        BigDecimal cost,
        int stock,
        int minStock,
        boolean lowStock,
        boolean active
) {}
