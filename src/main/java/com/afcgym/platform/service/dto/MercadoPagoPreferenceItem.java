package com.afcgym.platform.service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

public record MercadoPagoPreferenceItem(
        String id,
        String title,
        int quantity,
        @JsonProperty("currency_id") String currencyId,
        @JsonProperty("unit_price") BigDecimal unitPrice) {}
