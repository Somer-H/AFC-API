package com.afcgym.platform.web.dto.request;

import jakarta.validation.constraints.Pattern;

public record RegisterMemberCredentialsRequest(
        @Pattern(regexp = "\\d{4}") String pin,
        String fingerprintToken
) {}
