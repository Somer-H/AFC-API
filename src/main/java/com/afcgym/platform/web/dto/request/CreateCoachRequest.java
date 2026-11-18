package com.afcgym.platform.web.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCoachRequest(
        @NotBlank @Size(max = 150) String name,
        @Size(max = 500) String avatarUrl
) {}
