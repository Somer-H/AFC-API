package com.afcgym.platform.configuration;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.email")
public record EmailProperties(
        String host,
        @Min(1) @Max(65535) int port,
        String username,
        String password,
        String from,
        String passwordResetUrl) {

    public boolean isConfigured() {
        return hasText(host) && hasText(from);
    }

    public boolean hasCredentials() {
        return hasText(username);
    }

    public boolean hasPasswordResetUrl() {
        return hasText(passwordResetUrl);
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
