package com.afcgym.platform.web.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import lombok.Builder;

@Builder
public record SaleResponse(
        Long id,
        String folio,
        BigDecimal total,
        String paymentMethod,
        Long cashierId,
        Instant createdAt,
        List<SaleItemResponse> items
) {}
