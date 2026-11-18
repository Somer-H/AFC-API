package com.afcgym.platform.web.dto.response;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import lombok.Builder;

@Builder
public record BookingResponse(
        Long id,
        Long scheduleBlockId,
        Long memberId,
        LocalDate classDate,
        String dayOfWeek,
        LocalTime startTime,
        LocalTime endTime,
        String status,
        Instant createdAt,
        Instant cancelledAt
) {}
