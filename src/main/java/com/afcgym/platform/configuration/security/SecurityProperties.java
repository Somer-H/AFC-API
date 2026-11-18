package com.afcgym.platform.configuration.security;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.security")
public record SecurityProperties(
        String jwtSecret,
        long jwtTtlMinutes,
        int bcryptStrength,
        List<String> allowedOrigins
) {}
