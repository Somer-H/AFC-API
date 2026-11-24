package com.afcgym.platform.web.dto.response;

import java.time.LocalDate;
import java.util.List;
import lombok.Builder;

@Builder
public record DayOccupancyResponse(
        LocalDate date,
        String dayOfWeek,
        List<BlockOccupancyResponse> blocks
) {}
