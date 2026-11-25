package com.afcgym.platform.service.dto;

import com.afcgym.platform.persistance.entities.enums.AuditOutcome;
import java.time.LocalDate;

public record AuditLogFilter(Long actorUserId, String action, AuditOutcome outcome, LocalDate from, LocalDate to) {}
