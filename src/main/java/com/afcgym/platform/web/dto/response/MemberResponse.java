package com.afcgym.platform.web.dto.response;

import java.time.LocalDate;
import lombok.Builder;

@Builder
public record MemberResponse(
        Long id,
        String code,
        String fullName,
        String phone,
        String email,
        String membershipType,
        LocalDate dueDate,
        String status,
        boolean active,
        Long userId
) {}
