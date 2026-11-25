package com.afcgym.platform.web.dto.response;

import java.time.Instant;
import lombok.Builder;

@Builder
public record AuditLogResponse(
        Long id,
        Instant occurredAt,
        Long actorUserId,
        String actorRole,
        String action,
        String httpMethod,
        String path,
        String targetId,
        String outcome,
        int responseStatus
) {}
