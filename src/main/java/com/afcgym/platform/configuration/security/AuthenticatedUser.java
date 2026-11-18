package com.afcgym.platform.configuration.security;

import com.afcgym.platform.persistance.entities.enums.Role;
import org.springframework.security.oauth2.jwt.Jwt;

public record AuthenticatedUser(Long userId, Role role) {

    public static AuthenticatedUser fromJwt(Jwt jwt) {
        Long userId = jwt.getClaim("userId");
        Role role = Role.valueOf(jwt.getClaimAsString("role"));
        return new AuthenticatedUser(userId, role);
    }
}
