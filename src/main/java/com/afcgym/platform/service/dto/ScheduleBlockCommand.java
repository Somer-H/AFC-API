package com.afcgym.platform.service.dto;

import java.time.DayOfWeek;
import java.time.LocalTime;

public record ScheduleBlockCommand(
        DayOfWeek dayOfWeek, LocalTime startTime, LocalTime endTime, int maxCapacity, Long coachId) {}
