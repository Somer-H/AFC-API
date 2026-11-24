package com.afcgym.platform.web.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Builder;

@Builder
public record AppointmentAgendaResponse(
        Long id,
        LocalDateTime scheduledAt,
        String status,
        Long paymentId,
        String paymentReference,
        String paymentStatus,
        String membershipType,
        BigDecimal amount,
        Long memberId,
        String memberCode,
        String memberName,
        String memberPhone
) {}
