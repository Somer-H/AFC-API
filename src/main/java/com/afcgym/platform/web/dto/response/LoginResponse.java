package com.afcgym.platform.web.dto.response;

import lombok.Builder;

@Builder
public record LoginResponse(
        String token,
        String role,
        long expiresInSeconds
) {}
