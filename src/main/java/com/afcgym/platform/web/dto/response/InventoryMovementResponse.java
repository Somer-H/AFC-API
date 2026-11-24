package com.afcgym.platform.web.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import lombok.Builder;

@Builder
public record InventoryMovementResponse(
        Long id,
        Long productId,
        String type,
        int quantity,
        BigDecimal unitCost,
        int finalStock,
        Long userId,
        Instant createdAt
) {}
