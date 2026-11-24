package com.afcgym.platform.web.dto.request;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public record RescheduleAppointmentRequest(@NotNull LocalDateTime appointmentAt) {}
