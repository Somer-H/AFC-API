package com.afcgym.platform.service.dto;

import java.time.DayOfWeek;
import java.util.List;

public record RoutineCommand(
        DayOfWeek dayOfWeek, String title, String description, Long coachId, List<ExerciseCommand> exercises) {}
