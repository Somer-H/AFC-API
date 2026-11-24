package com.afcgym.platform.service.dto;

import com.afcgym.platform.persistance.entities.enums.Role;

public record LoginResult(
        String token,
        Role role,
        long expiresInSeconds
) {}
