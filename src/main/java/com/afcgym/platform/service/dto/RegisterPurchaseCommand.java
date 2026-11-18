package com.afcgym.platform.service.dto;

import java.math.BigDecimal;

public record RegisterPurchaseCommand(
        int quantity,
        BigDecimal unitCost,
        Long userId
) {}
