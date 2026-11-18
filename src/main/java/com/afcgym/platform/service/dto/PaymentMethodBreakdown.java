package com.afcgym.platform.service.dto;

import com.afcgym.platform.persistance.entities.enums.PaymentMethod;
import java.math.BigDecimal;

public record PaymentMethodBreakdown(PaymentMethod method, BigDecimal amount, BigDecimal percentage, int transactions) {}
