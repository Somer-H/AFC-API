package com.afcgym.platform.web.controllers;

import com.afcgym.platform.mapper.DashboardMapper;
import com.afcgym.platform.service.IDashboardService;
import com.afcgym.platform.web.dto.response.DashboardSummaryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard")
public class DashboardController {

    private final IDashboardService dashboardService;
    private final DashboardMapper dashboardMapper;

    @GetMapping("/summary")
    @Operation(summary = "Gym KPIs: members by status, income of the month, store sales by category and alerts")
    public DashboardSummaryResponse summary(@RequestParam(required = false) Integer expiringWithinDays) {
        return dashboardMapper.toResponse(dashboardService.summary(expiringWithinDays));
    }
}
