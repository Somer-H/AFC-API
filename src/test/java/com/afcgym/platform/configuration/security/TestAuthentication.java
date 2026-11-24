package com.afcgym.platform.configuration.security;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

import com.afcgym.platform.persistance.entities.enums.Role;
import java.util.UUID;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

public final class TestAuthentication {

    private TestAuthentication() {
    }

    public static RequestPostProcessor asRole(Role role) {
        return jwt()
                .jwt(builder -> builder.claim("role", role.name()).claim("jti", UUID.randomUUID().toString()))
                .authorities(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    public static RequestPostProcessor asUser(Role role, Long userId) {
        return jwt()
                .jwt(builder -> builder.claim("role", role.name())
                        .claim("userId", userId)
                        .claim("jti", UUID.randomUUID().toString()))
                .authorities(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }
}
