package com.afcgym.platform.service.dto;

import java.time.LocalDateTime;

public record CancelAppointmentCommand(String reason, LocalDateTime suggestedAt) {}
