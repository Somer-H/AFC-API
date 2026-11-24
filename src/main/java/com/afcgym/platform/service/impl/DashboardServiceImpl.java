package com.afcgym.platform.service.impl;

import com.afcgym.platform.exception.BadRequestException;
import com.afcgym.platform.persistance.entities.CategoryEntity;
import com.afcgym.platform.persistance.entities.ProductEntity;
import com.afcgym.platform.persistance.entities.SaleEntity;
import com.afcgym.platform.persistance.entities.SaleItemEntity;
import com.afcgym.platform.persistance.entities.enums.MemberStatus;
import com.afcgym.platform.persistance.repositories.ICategoryRepository;
import com.afcgym.platform.persistance.repositories.IMemberRepository;
import com.afcgym.platform.persistance.repositories.IProductRepository;
import com.afcgym.platform.persistance.repositories.ISaleRepository;
import com.afcgym.platform.persistance.repositories.specifications.ProductSpecifications;
import com.afcgym.platform.service.ICashCutService;
import com.afcgym.platform.service.IDashboardService;
import com.afcgym.platform.service.dto.CashCutCategory;
import com.afcgym.platform.service.dto.CashCutPeriod;
import com.afcgym.platform.service.dto.CashCutReport;
import com.afcgym.platform.service.dto.DashboardSummary;
import com.afcgym.platform.service.dto.DashboardSummary.Alerts;
import com.afcgym.platform.service.dto.DashboardSummary.CategorySales;
import com.afcgym.platform.service.dto.DashboardSummary.IncomeSummary;
import com.afcgym.platform.service.dto.DashboardSummary.MemberCounts;
import com.afcgym.platform.service.dto.DashboardSummary.MonthlyIncome;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements IDashboardService {

    private static final int DEFAULT_EXPIRING_WITHIN_DAYS = 3;
    private static final int MAX_EXPIRING_WITHIN_DAYS = 30;
    private static final int MONTHS_IN_CHART = 6;
    private static final String NO_CATEGORY = "Sin categoría";
    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);

    private final IMemberRepository memberRepository;
    private final IProductRepository productRepository;
    private final ICategoryRepository categoryRepository;
    private final ISaleRepository saleRepository;
    private final ICashCutService cashCutService;

    @Override
    @Transactional(readOnly = true)
    public DashboardSummary summary(Integer expiringWithinDays) {
        int days = expiringWithinDays != null ? expiringWithinDays : DEFAULT_EXPIRING_WITHIN_DAYS;
        if (days < 1 || days > MAX_EXPIRING_WITHIN_DAYS) {
            throw new BadRequestException(
                    "Los días para avisar de vencimientos deben estar entre 1 y " + MAX_EXPIRING_WITHIN_DAYS);
        }
        LocalDate today = LocalDate.now();
        List<MonthlyIncome> monthlyIncome = monthlyIncome(today);
        return new DashboardSummary(
                today,
                memberCounts(today),
                incomeSummary(monthlyIncome),
                monthlyIncome,
                storeSalesByCategory(today),
                new Alerts(
                        days,
                        memberRepository.findByActiveTrueAndDueDateBetweenOrderByDueDateAsc(today, today.plusDays(days)),
                        lowStockProducts()));
    }

    private MemberCounts memberCounts(LocalDate today) {
        LocalDate oldestExpired = today.minusDays(MemberStatus.TO_PURGE_THRESHOLD_DAYS);
        return new MemberCounts(
                memberRepository.countByActiveTrueAndDueDateGreaterThanEqual(today),
                memberRepository.countByActiveTrueAndDueDateBetween(oldestExpired, today.minusDays(1)),
                memberRepository.countByActiveTrueAndDueDateLessThan(oldestExpired),
                memberRepository.countByCreatedAtGreaterThanEqual(
                        today.withDayOfMonth(1).atStartOfDay(ZoneId.systemDefault()).toInstant()));
    }

    private List<MonthlyIncome> monthlyIncome(LocalDate today) {
        YearMonth current = YearMonth.from(today);
        List<MonthlyIncome> months = new ArrayList<>();
        for (int offset = MONTHS_IN_CHART - 1; offset >= 0; offset--) {
            YearMonth month = current.minusMonths(offset);
            LocalDate reference = offset == 0 ? today : month.atEndOfMonth();
            CashCutReport report = cashCutService.generate(CashCutPeriod.MONTHLY, CashCutCategory.ALL, reference);
            months.add(new MonthlyIncome(
                    month, report.membershipIncome(), report.storeIncome(), report.totalIncome()));
        }
        return months;
    }

    private IncomeSummary incomeSummary(List<MonthlyIncome> monthlyIncome) {
        MonthlyIncome current = monthlyIncome.getLast();
        MonthlyIncome previous = monthlyIncome.get(monthlyIncome.size() - 2);
        BigDecimal changePercent = previous.total().signum() == 0
                ? null
                : current.total().subtract(previous.total())
                        .multiply(ONE_HUNDRED)
                        .divide(previous.total(), 2, RoundingMode.HALF_UP);
        return new IncomeSummary(
                current.month(),
                current.membershipIncome(),
                current.storeIncome(),
                current.total(),
                previous.total(),
                changePercent);
    }

    private List<CategorySales> storeSalesByCategory(LocalDate today) {
        ZoneId zone = ZoneId.systemDefault();
        List<SaleItemEntity> items = saleRepository
                .findByCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByCreatedAtDesc(
                        today.withDayOfMonth(1).atStartOfDay(zone).toInstant(),
                        today.plusDays(1).atStartOfDay(zone).toInstant())
                .stream()
                .map(SaleEntity::getItems)
                .flatMap(List::stream)
                .toList();
        if (items.isEmpty()) {
            return List.of();
        }
        Set<Long> productIds = items.stream().map(SaleItemEntity::getProductId).collect(Collectors.toSet());
        Map<Long, Long> categoryByProduct = new HashMap<>();
        productRepository.findAllById(productIds).forEach(product ->
                categoryByProduct.put(product.getId(), product.getCategoryId()));
        Map<Long, String> categoryNames = categoryRepository.findAllById(categoryByProduct.values().stream()
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet()))
                .stream()
                .collect(Collectors.toMap(CategoryEntity::getId, CategoryEntity::getName));

        Map<String, List<SaleItemEntity>> itemsByCategory = items.stream()
                .collect(Collectors.groupingBy(item -> {
                    Long categoryId = categoryByProduct.get(item.getProductId());
                    return categoryId == null ? NO_CATEGORY : categoryNames.getOrDefault(categoryId, NO_CATEGORY);
                }));
        return itemsByCategory.entrySet().stream()
                .map(entry -> new CategorySales(
                        entry.getKey(),
                        entry.getValue().stream()
                                .map(SaleItemEntity::getSubtotal)
                                .reduce(BigDecimal.ZERO, BigDecimal::add)
                                .setScale(2, RoundingMode.HALF_UP),
                        entry.getValue().stream().mapToInt(SaleItemEntity::getQuantity).sum()))
                .sorted(Comparator.comparing(CategorySales::amount).reversed()
                        .thenComparing(CategorySales::category))
                .toList();
    }

    private List<ProductEntity> lowStockProducts() {
        return productRepository.findAll(
                ProductSpecifications.active().and(ProductSpecifications.lowStock()),
                Sort.by("stock", "name"));
    }
}
