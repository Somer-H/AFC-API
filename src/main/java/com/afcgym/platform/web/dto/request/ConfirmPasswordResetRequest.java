package com.afcgym.platform.web.dto.request;

import jakarta.validation.constraints.NotBlank;

public record ConfirmPasswordResetRequest(
        @NotBlank String token,
        @NotBlank String newPassword
) {}
