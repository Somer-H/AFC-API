package com.afcgym.platform.web.dto.response;

import java.time.LocalTime;
import lombok.Builder;

@Builder
public record ScheduleBlockResponse(
        Long id,
        String dayOfWeek,
        LocalTime startTime,
        LocalTime endTime,
        String shift,
        int maxCapacity,
        boolean active,
        CoachResponse coach
) {}
