package com.afcgym.platform.configuration.security;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.security")
public record SecurityProperties(
        String jwtSecret,
        @Min(5) @Max(1440) long jwtTtlMinutes,
        @Min(1) @Max(168) long sessionMaxHours,
        int bcryptStrength,
        List<String> allowedOrigins
) {}
