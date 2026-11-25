package com.afcgym.platform.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.afcgym.platform.exception.BadRequestException;
import com.afcgym.platform.persistance.entities.PaymentEntity;
import com.afcgym.platform.persistance.entities.SaleEntity;
import com.afcgym.platform.persistance.entities.enums.MembershipType;
import com.afcgym.platform.persistance.entities.enums.PaymentMethod;
import com.afcgym.platform.persistance.entities.enums.PaymentStatus;
import com.afcgym.platform.persistance.repositories.IPaymentRepository;
import com.afcgym.platform.persistance.repositories.ISaleRepository;
import com.afcgym.platform.service.dto.CashCutCategory;
import com.afcgym.platform.service.dto.ExportedFile;
import com.afcgym.platform.service.dto.CashCutPeriod;
import com.afcgym.platform.service.dto.CashCutReport;
import com.afcgym.platform.service.dto.PaymentMethodBreakdown;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CashCutServiceImplTest {

    private static final LocalDate WEDNESDAY = LocalDate.of(2026, 11, 11);

    @Mock
    private IPaymentRepository paymentRepository;

    @Mock
    private ISaleRepository saleRepository;

    @InjectMocks
    private CashCutServiceImpl cashCutService;

    @Test
    void givenPaymentsAndSales_whenGenerateForAll_thenAddBothAndRecalculatePercentagesOverTheCombinedTotal() {
        when(paymentRepository.findByStatusAndApprovedAtGreaterThanEqualAndApprovedAtLessThan(
                eq(PaymentStatus.APPROVED), any(Instant.class), any(Instant.class)))
                .thenReturn(List.of(payment(PaymentMethod.CARD, "500.00"), payment(PaymentMethod.CASH, "300.00")));
        when(saleRepository.findByCreatedAtGreaterThanEqualAndCreatedAtLessThan(any(Instant.class), any(Instant.class)))
                .thenReturn(List.of(sale(PaymentMethod.CASH, "200.00"), sale(PaymentMethod.SPEI, "100.00")));

        CashCutReport report = cashCutService.generate(CashCutPeriod.DAILY, CashCutCategory.ALL, WEDNESDAY);

        assertMoney("800.00", report.membershipIncome());
        assertMoney("300.00", report.storeIncome());
        assertMoney("1100.00", report.totalIncome());
        assertEquals(2, report.membershipPayments());
        assertEquals(2, report.storeSales());
        assertBreakdown(report, PaymentMethod.CARD, "500.00", "45.45", 1);
        assertBreakdown(report, PaymentMethod.CASH, "500.00", "45.45", 2);
        assertBreakdown(report, PaymentMethod.SPEI, "100.00", "9.09", 1);
    }

    @Test
    void givenOnlyMemberships_whenGenerate_thenIgnoreTheStoreAndUseTheMembershipTotalForPercentages() {
        when(paymentRepository.findByStatusAndApprovedAtGreaterThanEqualAndApprovedAtLessThan(
                eq(PaymentStatus.APPROVED), any(Instant.class), any(Instant.class)))
                .thenReturn(List.of(payment(PaymentMethod.CARD, "500.00"), payment(PaymentMethod.CASH, "500.00")));

        CashCutReport report = cashCutService.generate(CashCutPeriod.DAILY, CashCutCategory.MEMBERSHIPS, WEDNESDAY);

        assertNull(report.storeIncome());
        assertMoney("1000.00", report.totalIncome());
        assertBreakdown(report, PaymentMethod.CARD, "500.00", "50.00", 1);
        assertBreakdown(report, PaymentMethod.SPEI, "0.00", "0.00", 0);
        verifyNoInteractions(saleRepository);
    }

    @Test
    void givenOnlyTheStore_whenGenerate_thenIgnoreMembershipPayments() {
        when(saleRepository.findByCreatedAtGreaterThanEqualAndCreatedAtLessThan(any(Instant.class), any(Instant.class)))
                .thenReturn(List.of(sale(PaymentMethod.CASH, "150.00")));

        CashCutReport report = cashCutService.generate(CashCutPeriod.DAILY, CashCutCategory.STORE, WEDNESDAY);

        assertNull(report.membershipIncome());
        assertMoney("150.00", report.storeIncome());
        assertBreakdown(report, PaymentMethod.CASH, "150.00", "100.00", 1);
        verifyNoInteractions(paymentRepository);
    }

    @Test
    void givenNoMovements_whenGenerate_thenListEveryMethodWithZeroInsteadOfDividingByZero() {
        when(paymentRepository.findByStatusAndApprovedAtGreaterThanEqualAndApprovedAtLessThan(
                eq(PaymentStatus.APPROVED), any(Instant.class), any(Instant.class)))
                .thenReturn(List.of());
        when(saleRepository.findByCreatedAtGreaterThanEqualAndCreatedAtLessThan(any(Instant.class), any(Instant.class)))
                .thenReturn(List.of());

        CashCutReport report = cashCutService.generate(CashCutPeriod.MONTHLY, CashCutCategory.ALL, WEDNESDAY);

        assertMoney("0.00", report.totalIncome());
        assertEquals(PaymentMethod.values().length, report.paymentMethods().size());
        assertBreakdown(report, PaymentMethod.CARD, "0.00", "0.00", 0);
    }

    @Test
    void givenAWeeklyPeriod_whenGenerate_thenQueryFromMondayToTheStartOfTheNextMonday() {
        ZoneId zone = ZoneId.systemDefault();
        Instant monday = LocalDate.of(2026, 11, 9).atStartOfDay(zone).toInstant();
        Instant nextMonday = LocalDate.of(2026, 11, 16).atStartOfDay(zone).toInstant();
        when(paymentRepository.findByStatusAndApprovedAtGreaterThanEqualAndApprovedAtLessThan(
                PaymentStatus.APPROVED, monday, nextMonday))
                .thenReturn(List.of());

        CashCutReport report = cashCutService.generate(CashCutPeriod.WEEKLY, CashCutCategory.MEMBERSHIPS, WEDNESDAY);

        assertEquals(LocalDate.of(2026, 11, 9), report.from());
        assertEquals(LocalDate.of(2026, 11, 15), report.to());
    }

    @Test
    void givenAFutureDate_whenGenerate_thenThrowBadRequestException() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);

        assertThrows(BadRequestException.class,
                () -> cashCutService.generate(CashCutPeriod.DAILY, CashCutCategory.ALL, tomorrow));

        verifyNoInteractions(paymentRepository, saleRepository);
    }

    @Test
    void givenNoDate_whenGenerate_thenUseToday() {
        when(saleRepository.findByCreatedAtGreaterThanEqualAndCreatedAtLessThan(any(Instant.class), any(Instant.class)))
                .thenReturn(List.of());

        CashCutReport report = cashCutService.generate(CashCutPeriod.DAILY, CashCutCategory.STORE, null);

        assertEquals(LocalDate.now(), report.from());
    }

    @Test
    void givenAReport_whenExport_thenReturnAnXlsxNamedAfterTheFilters() {
        when(saleRepository.findByCreatedAtGreaterThanEqualAndCreatedAtLessThan(any(Instant.class), any(Instant.class)))
                .thenReturn(List.of(sale(PaymentMethod.CASH, "150.00")));

        ExportedFile export = cashCutService.export(CashCutPeriod.WEEKLY, CashCutCategory.STORE, WEDNESDAY);

        assertEquals("corte-de-caja_semanal_tienda_2026-11-09_2026-11-15.xlsx", export.fileName());
        assertTrue(export.content().length > 0);
        assertEquals('P', (char) export.content()[0]);
        assertEquals('K', (char) export.content()[1]);
    }

    private void assertBreakdown(
            CashCutReport report, PaymentMethod method, String amount, String percentage, int transactions) {
        PaymentMethodBreakdown breakdown = report.paymentMethods().stream()
                .filter(item -> item.method() == method)
                .findFirst()
                .orElseThrow();
        assertMoney(amount, breakdown.amount());
        assertMoney(percentage, breakdown.percentage());
        assertEquals(transactions, breakdown.transactions());
    }

    private void assertMoney(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual), "expected " + expected + " but was " + actual);
    }

    private PaymentEntity payment(PaymentMethod method, String amount) {
        return PaymentEntity.builder()
                .memberId(1L)
                .membershipType(MembershipType.MONTHLY)
                .method(method)
                .status(PaymentStatus.APPROVED)
                .amount(new BigDecimal(amount))
                .createdAt(Instant.now())
                .approvedAt(Instant.now())
                .build();
    }

    private SaleEntity sale(PaymentMethod method, String total) {
        return SaleEntity.builder()
                .folio("V-000001")
                .total(new BigDecimal(total))
                .paymentMethod(method)
                .createdAt(Instant.now())
                .build();
    }
}
