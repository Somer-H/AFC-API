package com.afcgym.platform.web.dto.response;

import lombok.Builder;

@Builder
public record UserResponse(
        Long id,
        String username,
        String fullName,
        String email,
        String role,
        boolean active
) {}
