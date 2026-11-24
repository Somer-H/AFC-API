package com.afcgym.platform.web.dto.response;

import lombok.Builder;

@Builder
public record CoachResponse(
        Long id,
        String name,
        String avatarUrl
) {}
