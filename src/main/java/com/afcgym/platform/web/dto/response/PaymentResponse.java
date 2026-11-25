package com.afcgym.platform.web.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import lombok.Builder;

@Builder
public record PaymentResponse(
        Long id,
        String reference,
        Long memberId,
        String membershipType,
        String method,
        String status,
        BigDecimal amount,
        int discountPercent,
        String cardLastFour,
        LocalDate scheduledDate,
        Instant createdAt,
        Instant approvedAt,
        Long approvedBy,
        Instant rejectedAt,
        Long rejectedBy,
        String rejectionReason
) {}
