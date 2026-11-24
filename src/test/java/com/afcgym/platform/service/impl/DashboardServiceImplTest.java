package com.afcgym.platform.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.afcgym.platform.exception.BadRequestException;
import com.afcgym.platform.persistance.entities.CategoryEntity;
import com.afcgym.platform.persistance.entities.ProductEntity;
import com.afcgym.platform.persistance.entities.SaleEntity;
import com.afcgym.platform.persistance.entities.SaleItemEntity;
import com.afcgym.platform.persistance.entities.enums.PaymentMethod;
import com.afcgym.platform.persistance.repositories.ICategoryRepository;
import com.afcgym.platform.persistance.repositories.IMemberRepository;
import com.afcgym.platform.persistance.repositories.IProductRepository;
import com.afcgym.platform.persistance.repositories.ISaleRepository;
import com.afcgym.platform.service.ICashCutService;
import com.afcgym.platform.service.dto.CashCutCategory;
import com.afcgym.platform.service.dto.CashCutPeriod;
import com.afcgym.platform.service.dto.CashCutReport;
import com.afcgym.platform.service.dto.DashboardSummary;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class DashboardServiceImplTest {

    @Mock
    private IMemberRepository memberRepository;

    @Mock
    private IProductRepository productRepository;

    @Mock
    private ICategoryRepository categoryRepository;

    @Mock
    private ISaleRepository saleRepository;

    @Mock
    private ICashCutService cashCutService;

    @InjectMocks
    private DashboardServiceImpl dashboardService;

    private LocalDate today;

    @BeforeEach
    void init() {
        today = LocalDate.now();
        lenient().when(memberRepository.findByActiveTrueAndDueDateBetweenOrderByDueDateAsc(any(), any()))
                .thenReturn(List.of());
        lenient().when(productRepository.findAll(any(Specification.class), any(Sort.class))).thenReturn(List.of());
        lenient().when(saleRepository.findByCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByCreatedAtDesc(
                any(), any())).thenReturn(List.of());
        lenient().when(cashCutService.generate(eq(CashCutPeriod.MONTHLY), eq(CashCutCategory.ALL), any()))
                .thenAnswer(invocation -> report(invocation.getArgument(2), "0.00", "0.00"));
    }

    @Test
    void givenDefaultSettings_whenSummary_thenCountMembersWithTheSameBoundariesAsMemberStatus() {
        when(memberRepository.countByActiveTrueAndDueDateGreaterThanEqual(today)).thenReturn(140L);
        when(memberRepository.countByActiveTrueAndDueDateBetween(today.minusDays(60), today.minusDays(1)))
                .thenReturn(18L);
        when(memberRepository.countByActiveTrueAndDueDateLessThan(today.minusDays(60))).thenReturn(4L);
        when(memberRepository.countByCreatedAtGreaterThanEqual(
                today.withDayOfMonth(1).atStartOfDay(ZoneId.systemDefault()).toInstant())).thenReturn(5L);

        DashboardSummary summary = dashboardService.summary(null);

        assertEquals(140L, summary.members().active());
        assertEquals(18L, summary.members().expired());
        assertEquals(4L, summary.members().toPurge());
        assertEquals(5L, summary.members().newThisMonth());
        assertEquals(3, summary.alerts().expiringWithinDays());
        verify(memberRepository).findByActiveTrueAndDueDateBetweenOrderByDueDateAsc(today, today.plusDays(3));
    }

    @Test
    void givenIncomeThisAndLastMonth_whenSummary_thenReportTheMonthAndItsChange() {
        YearMonth current = YearMonth.from(today);
        when(cashCutService.generate(eq(CashCutPeriod.MONTHLY), eq(CashCutCategory.ALL), any()))
                .thenAnswer(invocation -> {
                    YearMonth month = YearMonth.from((LocalDate) invocation.getArgument(2));
                    if (month.equals(current)) {
                        return report(invocation.getArgument(2), "1000.00", "200.00");
                    }
                    if (month.equals(current.minusMonths(1))) {
                        return report(invocation.getArgument(2), "900.00", "100.00");
                    }
                    return report(invocation.getArgument(2), "0.00", "0.00");
                });

        DashboardSummary summary = dashboardService.summary(null);

        assertEquals(current, summary.income().month());
        assertMoney("1200.00", summary.income().total());
        assertMoney("1000.00", summary.income().previousMonthTotal());
        assertMoney("20.00", summary.income().changePercent());
        assertEquals(6, summary.monthlyIncome().size());
        assertEquals(current.minusMonths(5), summary.monthlyIncome().getFirst().month());
        assertEquals(current, summary.monthlyIncome().getLast().month());
        verify(cashCutService).generate(CashCutPeriod.MONTHLY, CashCutCategory.ALL, today);
    }

    @Test
    void givenNoIncomeLastMonth_whenSummary_thenLeaveTheChangeEmptyInsteadOfDividingByZero() {
        DashboardSummary summary = dashboardService.summary(null);

        assertNull(summary.income().changePercent());
    }

    @Test
    void givenSalesInSeveralCategories_whenSummary_thenGroupThemBiggestFirst() {
        SaleEntity sale = SaleEntity.builder()
                .folio("V-000001")
                .total(new BigDecimal("1150.00"))
                .paymentMethod(PaymentMethod.CASH)
                .createdAt(Instant.now())
                .items(List.of(item(1L, 2, "900.00"), item(2L, 1, "150.00"), item(3L, 4, "100.00")))
                .build();
        when(saleRepository.findByCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByCreatedAtDesc(any(), any()))
                .thenReturn(List.of(sale));
        when(productRepository.findAllById(anyCollection())).thenReturn(List.of(
                product(1L, 10L), product(2L, 20L), product(3L, null)));
        when(categoryRepository.findAllById(anyCollection())).thenReturn(List.of(
                category(10L, "Proteínas"), category(20L, "Accesorios")));

        DashboardSummary summary = dashboardService.summary(null);

        assertEquals(3, summary.storeSalesByCategory().size());
        assertEquals("Proteínas", summary.storeSalesByCategory().get(0).category());
        assertMoney("900.00", summary.storeSalesByCategory().get(0).amount());
        assertEquals(2, summary.storeSalesByCategory().get(0).units());
        assertEquals("Accesorios", summary.storeSalesByCategory().get(1).category());
        assertEquals("Sin categoría", summary.storeSalesByCategory().get(2).category());
        assertEquals(4, summary.storeSalesByCategory().get(2).units());
    }

    @Test
    void givenACustomWindow_whenSummary_thenLookForExpiringMembershipsInThatWindow() {
        dashboardService.summary(7);

        verify(memberRepository).findByActiveTrueAndDueDateBetweenOrderByDueDateAsc(today, today.plusDays(7));
    }

    @Test
    void givenAWindowOutOfRange_whenSummary_thenThrowBadRequestException() {
        assertThrows(BadRequestException.class, () -> dashboardService.summary(0));
        assertThrows(BadRequestException.class, () -> dashboardService.summary(31));

        verifyNoInteractions(cashCutService);
    }

    private CashCutReport report(LocalDate reference, String membershipIncome, String storeIncome) {
        BigDecimal membership = new BigDecimal(membershipIncome);
        BigDecimal store = new BigDecimal(storeIncome);
        return new CashCutReport(CashCutPeriod.MONTHLY, CashCutCategory.ALL, reference.withDayOfMonth(1), reference,
                membership, store, membership.add(store), 0, 0, List.of());
    }

    private SaleItemEntity item(Long productId, int quantity, String subtotal) {
        return SaleItemEntity.builder()
                .productId(productId)
                .productName("Producto " + productId)
                .quantity(quantity)
                .unitPrice(new BigDecimal(subtotal))
                .subtotal(new BigDecimal(subtotal))
                .build();
    }

    private ProductEntity product(Long id, Long categoryId) {
        return ProductEntity.builder()
                .id(id)
                .code("SKU-" + id)
                .name("Producto " + id)
                .categoryId(categoryId)
                .price(BigDecimal.TEN)
                .stock(10)
                .minStock(5)
                .active(true)
                .createdAt(Instant.now())
                .build();
    }

    private CategoryEntity category(Long id, String name) {
        return CategoryEntity.builder().id(id).name(name).createdAt(Instant.now()).build();
    }

    private void assertMoney(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual), "expected " + expected + " but was " + actual);
    }
}
