package com.afcgym.platform.web.dto.response;

import java.time.Instant;
import java.time.LocalDate;
import lombok.Builder;

@Builder
public record AnnouncementResponse(
        Long id,
        String title,
        String message,
        String type,
        LocalDate startDate,
        LocalDate endDate,
        boolean current,
        Long createdBy,
        Instant createdAt
) {}
