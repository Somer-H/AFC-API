package com.afcgym.platform.service.dto;

import com.afcgym.platform.persistance.entities.enums.PaymentMethod;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record SalesReportLine(
        String folio,
        LocalDateTime soldAt,
        PaymentMethod paymentMethod,
        String products,
        String cashier,
        BigDecimal total
) {}
