package com.afcgym.platform.web.dto.response;

import java.time.Instant;
import java.util.List;
import lombok.Builder;

@Builder
public record RoutineResponse(
        Long id,
        String dayOfWeek,
        String title,
        String description,
        CoachResponse coach,
        List<ExerciseResponse> exercises,
        Instant createdAt
) {}
