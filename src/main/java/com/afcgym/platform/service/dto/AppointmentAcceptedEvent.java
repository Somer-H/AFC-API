package com.afcgym.platform.service.dto;

import java.time.LocalDateTime;

public record AppointmentAcceptedEvent(Long memberId, LocalDateTime scheduledAt) {}
