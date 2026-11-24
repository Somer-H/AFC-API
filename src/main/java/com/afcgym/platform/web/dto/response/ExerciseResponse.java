package com.afcgym.platform.web.dto.response;

import lombok.Builder;

@Builder
public record ExerciseResponse(
        Long id,
        String name,
        int sets,
        String reps,
        Integer restSeconds,
        String videoType,
        String videoUrl
) {}
