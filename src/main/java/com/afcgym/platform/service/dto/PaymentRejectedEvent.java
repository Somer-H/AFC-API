package com.afcgym.platform.service.dto;

public record PaymentRejectedEvent(Long memberId, String reason) {}
