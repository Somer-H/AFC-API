package com.afcgym.platform.configuration;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.notifications")
public record NotificationProperties(
        @NotBlank String expoPushUrl,
        String expoAccessToken,
        @Min(1) @Max(30) int expiryReminderDaysBefore) {

    public boolean hasExpoAccessToken() {
        return expoAccessToken != null && !expoAccessToken.isBlank();
    }
}
