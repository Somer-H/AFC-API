package com.afcgym.platform.web.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.DayOfWeek;
import java.util.List;

public record RoutineRequest(
        @NotNull DayOfWeek dayOfWeek,
        @NotBlank @Size(max = 150) String title,
        @Size(max = 1000) String description,
        @NotNull Long coachId,
        @NotEmpty @Size(max = 8) List<@NotNull @Valid ExerciseRequest> exercises
) {}
