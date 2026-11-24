package com.afcgym.platform.service.dto;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

public record DayOccupancy(LocalDate date, DayOfWeek dayOfWeek, List<BlockOccupancy> blocks) {}
