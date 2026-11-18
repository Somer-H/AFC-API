package com.afcgym.platform.web.dto.response;

import java.math.BigDecimal;
import lombok.Builder;

@Builder
public record BlockOccupancyResponse(
        ScheduleBlockResponse block,
        int booked,
        int available,
        BigDecimal occupancyPercentage
) {}
