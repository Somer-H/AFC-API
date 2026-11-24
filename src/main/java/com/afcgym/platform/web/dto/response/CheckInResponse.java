package com.afcgym.platform.web.dto.response;

import java.time.Instant;
import lombok.Builder;

@Builder
public record CheckInResponse(
        Long id,
        Long memberId,
        String method,
        Instant checkedInAt
) {}
