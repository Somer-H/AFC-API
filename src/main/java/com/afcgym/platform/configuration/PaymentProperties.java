package com.afcgym.platform.configuration;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.payments")
public record PaymentProperties(
        @Min(5) @Max(240) int appointmentMinutes,
        @Min(1) @Max(90) int appointmentMaxDaysAhead,
        @Valid @NotNull BankTransfer bankTransfer,
        @Valid @NotNull MercadoPago mercadoPago) {

    public record BankTransfer(String bankName, String beneficiary, String clabe) {

        public boolean isConfigured() {
            return hasText(bankName) && hasText(beneficiary) && clabe != null && clabe.matches("\\d{18}");
        }
    }

    public record MercadoPago(
            @NotBlank String apiUrl,
            String accessToken,
            String webhookSecret,
            String notificationUrl,
            String backUrl) {

        private static final String TEST_TOKEN_PREFIX = "TEST-";

        public boolean isConfigured() {
            return hasText(accessToken) && hasText(webhookSecret);
        }

        public boolean usesTestCredentials() {
            return hasText(accessToken) && accessToken.startsWith(TEST_TOKEN_PREFIX);
        }
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
