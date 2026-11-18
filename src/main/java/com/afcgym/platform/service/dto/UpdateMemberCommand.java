package com.afcgym.platform.service.dto;

import com.afcgym.platform.persistance.entities.enums.MembershipType;

public record UpdateMemberCommand(
        String fullName,
        String phone,
        String email,
        MembershipType membershipType
) {}
