package com.afcgym.platform.service.dto;

import java.math.BigDecimal;

public record GatewayPayment(
        String id,
        String status,
        String statusDetail,
        String externalReference,
        BigDecimal amount,
        String currency) {}
