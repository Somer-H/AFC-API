package com.afcgym.platform.service.dto;

import com.afcgym.platform.persistance.entities.enums.AnnouncementType;
import java.time.LocalDate;

public record PublishAnnouncementCommand(
        String title,
        String message,
        AnnouncementType type,
        LocalDate startDate,
        LocalDate endDate,
        Long createdBy
) {}
