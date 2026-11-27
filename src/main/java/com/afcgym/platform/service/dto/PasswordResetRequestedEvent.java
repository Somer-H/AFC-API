package com.afcgym.platform.service.dto;

import java.time.Instant;

public record PasswordResetRequestedEvent(String email, String fullName, String token, Instant expiresAt) {

    @Override
    public String toString() {
        return "PasswordResetRequestedEvent[email=" + email + ", expiresAt=" + expiresAt + "]";
    }
}
