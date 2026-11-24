package com.afcgym.platform.service;

import com.afcgym.platform.service.dto.DashboardSummary;

public interface IDashboardService {

    DashboardSummary summary(Integer expiringWithinDays);
}
