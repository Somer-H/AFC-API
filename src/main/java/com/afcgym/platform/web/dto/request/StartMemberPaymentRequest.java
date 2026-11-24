package com.afcgym.platform.web.dto.request;

import com.afcgym.platform.persistance.entities.enums.MembershipType;
import com.afcgym.platform.persistance.entities.enums.PaymentMethod;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public record StartMemberPaymentRequest(
        @NotNull MembershipType membershipType,
        @NotNull PaymentMethod method,
        LocalDateTime appointmentAt
) {}
