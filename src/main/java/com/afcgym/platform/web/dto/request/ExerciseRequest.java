package com.afcgym.platform.web.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record ExerciseRequest(
        Long id,
        @NotBlank @Size(max = 150) String name,
        @NotNull @Positive Integer sets,
        @NotBlank @Size(max = 30) String reps,
        @PositiveOrZero Integer restSeconds,
        @Size(max = 500) String videoUrl
) {}
