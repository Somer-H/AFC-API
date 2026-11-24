package com.afcgym.platform.web.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import lombok.Builder;

@Builder
public record DashboardSummaryResponse(
        LocalDate date,
        Members members,
        Income income,
        List<MonthlyIncome> monthlyIncome,
        List<CategorySales> storeSalesByCategory,
        Alerts alerts
) {

    @Builder
    public record Members(long active, long expired, long toPurge, long newThisMonth) {}

    @Builder
    public record Income(
            String month,
            BigDecimal membershipIncome,
            BigDecimal storeIncome,
            BigDecimal total,
            BigDecimal previousMonthTotal,
            BigDecimal changePercent
    ) {}

    @Builder
    public record MonthlyIncome(String month, BigDecimal membershipIncome, BigDecimal storeIncome, BigDecimal total) {}

    @Builder
    public record CategorySales(String category, BigDecimal amount, int units) {}

    @Builder
    public record Alerts(
            int expiringWithinDays,
            List<ExpiringMember> expiringMemberships,
            List<LowStockProduct> lowStockProducts
    ) {}

    @Builder
    public record ExpiringMember(Long id, String code, String fullName, String phone, LocalDate dueDate, long daysLeft) {}

    @Builder
    public record LowStockProduct(Long id, String code, String name, int stock, int minStock) {}
}
