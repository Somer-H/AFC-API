package com.afcgym.platform.service.dto;

import java.time.LocalDate;

public record BookClassCommand(Long scheduleBlockId, LocalDate classDate, Long memberId) {}
