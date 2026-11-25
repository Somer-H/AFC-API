package com.afcgym.platform.service.dto;

import com.afcgym.platform.persistance.entities.enums.AuditOutcome;
import com.afcgym.platform.persistance.entities.enums.Role;

public record AuditEntry(
        Long actorUserId,
        Role actorRole,
        String action,
        String httpMethod,
        String path,
        String targetId,
        AuditOutcome outcome,
        int responseStatus) {}
