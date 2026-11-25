package com.afcgym.platform.web.dto.request;

import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

public record CancelAppointmentRequest(@Size(max = 500) String reason, LocalDateTime suggestedAt) {}
