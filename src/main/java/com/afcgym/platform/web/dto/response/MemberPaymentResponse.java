package com.afcgym.platform.web.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import lombok.Builder;

@Builder
public record MemberPaymentResponse(
        Long id,
        String reference,
        String membershipType,
        String method,
        String status,
        BigDecimal amount,
        int discountPercent,
        Instant createdAt,
        Instant approvedAt,
        String rejectionReason,
        String checkoutUrl,
        BankTransferDetailsResponse bankTransfer,
        PaymentAppointmentResponse appointment
) {}
