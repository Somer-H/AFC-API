package com.afcgym.platform.web.dto.request;

import com.afcgym.platform.persistance.entities.enums.MembershipType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateMemberRequest(
        @NotBlank String fullName,
        String phone,
        @Email String email,
        @NotNull MembershipType membershipType
) {}
