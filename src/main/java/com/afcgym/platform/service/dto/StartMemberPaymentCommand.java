package com.afcgym.platform.service.dto;

import com.afcgym.platform.persistance.entities.enums.MembershipType;
import com.afcgym.platform.persistance.entities.enums.PaymentMethod;
import java.time.LocalDateTime;

public record StartMemberPaymentCommand(MembershipType membershipType, PaymentMethod method, LocalDateTime appointmentAt) {}
