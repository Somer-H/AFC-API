package com.afcgym.platform.web.dto.request;

import com.afcgym.platform.persistance.entities.enums.MembershipType;
import com.afcgym.platform.persistance.entities.enums.PaymentMethod;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDate;

public record RenewMembershipRequest(
        @NotNull MembershipType membershipType,
        @NotNull PaymentMethod method,
        @Pattern(regexp = "\\d{4}") String cardLastFour,
        LocalDate scheduledDate
) {}
