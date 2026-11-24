package com.afcgym.platform.web.dto.request;

import com.afcgym.platform.persistance.entities.enums.AnnouncementType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record PublishAnnouncementRequest(
        @NotBlank @Size(max = 150) String title,
        @NotBlank @Size(max = 2000) String message,
        @NotNull AnnouncementType type,
        @NotNull LocalDate startDate,
        LocalDate endDate
) {}
