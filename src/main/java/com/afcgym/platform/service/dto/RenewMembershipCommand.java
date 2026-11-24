package com.afcgym.platform.service.dto;

import com.afcgym.platform.persistance.entities.enums.MembershipType;
import com.afcgym.platform.persistance.entities.enums.PaymentMethod;
import java.time.LocalDate;

public record RenewMembershipCommand(
        MembershipType membershipType,
        PaymentMethod method,
        String cardLastFour,
        LocalDate scheduledDate
) {}
