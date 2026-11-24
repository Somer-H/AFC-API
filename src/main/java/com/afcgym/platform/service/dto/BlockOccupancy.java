package com.afcgym.platform.service.dto;

import com.afcgym.platform.persistance.entities.ScheduleBlockEntity;
import java.math.BigDecimal;

public record BlockOccupancy(ScheduleBlockEntity block, int booked, int available, BigDecimal occupancyPercentage) {}
