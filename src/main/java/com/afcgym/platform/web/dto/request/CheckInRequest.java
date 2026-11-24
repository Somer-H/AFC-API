package com.afcgym.platform.web.dto.request;

import jakarta.validation.constraints.NotNull;

public record CheckInRequest(
        @NotNull Long memberId,
        String pin,
        String fingerprintToken
) {}
