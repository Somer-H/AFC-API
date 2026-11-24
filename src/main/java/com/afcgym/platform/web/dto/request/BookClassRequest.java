package com.afcgym.platform.web.dto.request;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record BookClassRequest(
        @NotNull Long scheduleBlockId,
        @NotNull LocalDate classDate,
        Long memberId
) {}
