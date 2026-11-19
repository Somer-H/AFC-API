package com.afcgym.platform.service.dto;

public record ExerciseCommand(Long id, String name, int sets, String reps, Integer restSeconds, String videoUrl) {}
