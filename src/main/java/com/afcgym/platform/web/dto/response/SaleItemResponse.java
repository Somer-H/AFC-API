package com.afcgym.platform.web.dto.response;

import java.math.BigDecimal;
import lombok.Builder;

@Builder
public record SaleItemResponse(
        Long productId,
        String productName,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal subtotal
) {}
