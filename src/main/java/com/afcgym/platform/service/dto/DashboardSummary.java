package com.afcgym.platform.service.dto;

import com.afcgym.platform.persistance.entities.MemberEntity;
import com.afcgym.platform.persistance.entities.ProductEntity;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

public record DashboardSummary(
        LocalDate date,
        MemberCounts members,
        IncomeSummary income,
        List<MonthlyIncome> monthlyIncome,
        List<CategorySales> storeSalesByCategory,
        Alerts alerts
) {

    public record MemberCounts(long active, long expired, long toPurge, long newThisMonth) {}

    public record IncomeSummary(
            YearMonth month,
            BigDecimal membershipIncome,
            BigDecimal storeIncome,
            BigDecimal total,
            BigDecimal previousMonthTotal,
            BigDecimal changePercent
    ) {}

    public record MonthlyIncome(YearMonth month, BigDecimal membershipIncome, BigDecimal storeIncome, BigDecimal total) {}

    public record CategorySales(String category, BigDecimal amount, int units) {}

    public record Alerts(int expiringWithinDays, List<MemberEntity> expiringMembers, List<ProductEntity> lowStockProducts) {}
}
