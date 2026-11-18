package com.afcgym.platform.web.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CreateUserRequest(
        @NotBlank String username,
        @NotBlank String fullName,
        @NotBlank @Email String email,
        @NotBlank String password
) {}
