package com.afcgym.platform.web.dto.response;

import java.time.LocalDateTime;
import lombok.Builder;

@Builder
public record PaymentAppointmentResponse(
        Long id,
        Long paymentId,
        LocalDateTime scheduledAt,
        String status,
        String cancellationReason,
        LocalDateTime suggestedAt
) {}
