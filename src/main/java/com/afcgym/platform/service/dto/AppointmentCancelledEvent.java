package com.afcgym.platform.service.dto;

import java.time.LocalDateTime;

public record AppointmentCancelledEvent(Long memberId, String reason, LocalDateTime suggestedAt) {}
