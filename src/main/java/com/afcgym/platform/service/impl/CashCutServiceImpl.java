package com.afcgym.platform.service.impl;

import com.afcgym.platform.exception.BadRequestException;
import com.afcgym.platform.persistance.entities.PaymentEntity;
import com.afcgym.platform.persistance.entities.SaleEntity;
import com.afcgym.platform.persistance.entities.enums.PaymentMethod;
import com.afcgym.platform.persistance.entities.enums.PaymentStatus;
import com.afcgym.platform.persistance.repositories.IPaymentRepository;
import com.afcgym.platform.persistance.repositories.ISaleRepository;
import com.afcgym.platform.service.ICashCutService;
import com.afcgym.platform.service.dto.CashCutCategory;
import com.afcgym.platform.service.dto.ExportedFile;
import com.afcgym.platform.service.dto.CashCutPeriod;
import com.afcgym.platform.service.dto.CashCutReport;
import com.afcgym.platform.service.dto.PaymentMethodBreakdown;
import com.afcgym.platform.utils.CashCutWorkbookWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CashCutServiceImpl implements ICashCutService {

    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);

    private final IPaymentRepository paymentRepository;
    private final ISaleRepository saleRepository;

    @Override
    @Transactional(readOnly = true)
    public CashCutReport generate(CashCutPeriod period, CashCutCategory category, LocalDate reference) {
        LocalDate today = LocalDate.now();
        LocalDate referenceDate = reference != null ? reference : today;
        if (referenceDate.isAfter(today)) {
            throw new BadRequestException("La fecha del corte no puede ser futura");
        }
        LocalDate from = period.startOf(referenceDate);
        LocalDate to = period.endOf(referenceDate);
        ZoneId zone = ZoneId.systemDefault();
        Instant start = from.atStartOfDay(zone).toInstant();
        Instant end = to.plusDays(1).atStartOfDay(zone).toInstant();

        Map<PaymentMethod, BigDecimal> amounts = new EnumMap<>(PaymentMethod.class);
        Map<PaymentMethod, Integer> transactions = new EnumMap<>(PaymentMethod.class);
        for (PaymentMethod method : PaymentMethod.values()) {
            amounts.put(method, BigDecimal.ZERO);
            transactions.put(method, 0);
        }

        BigDecimal membershipIncome = null;
        int membershipPayments = 0;
        if (category.includesMemberships()) {
            List<PaymentEntity> payments = paymentRepository
                    .findByStatusAndApprovedAtGreaterThanEqualAndApprovedAtLessThan(PaymentStatus.APPROVED, start, end);
            membershipIncome = BigDecimal.ZERO;
            for (PaymentEntity payment : payments) {
                membershipIncome = membershipIncome.add(payment.getAmount());
                accumulate(amounts, transactions, payment.getMethod(), payment.getAmount());
            }
            membershipPayments = payments.size();
        }

        BigDecimal storeIncome = null;
        int storeSales = 0;
        if (category.includesStore()) {
            List<SaleEntity> sales = saleRepository.findByCreatedAtGreaterThanEqualAndCreatedAtLessThan(start, end);
            storeIncome = BigDecimal.ZERO;
            for (SaleEntity sale : sales) {
                storeIncome = storeIncome.add(sale.getTotal());
                accumulate(amounts, transactions, sale.getPaymentMethod(), sale.getTotal());
            }
            storeSales = sales.size();
        }

        BigDecimal totalIncome = amounts.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        List<PaymentMethodBreakdown> paymentMethods = Arrays.stream(PaymentMethod.values())
                .map(method -> new PaymentMethodBreakdown(
                        method,
                        money(amounts.get(method)),
                        percentage(amounts.get(method), totalIncome),
                        transactions.get(method)))
                .toList();

        return new CashCutReport(
                period,
                category,
                from,
                to,
                money(membershipIncome),
                money(storeIncome),
                money(totalIncome),
                membershipPayments,
                storeSales,
                paymentMethods);
    }

    @Override
    @Transactional(readOnly = true)
    public ExportedFile export(CashCutPeriod period, CashCutCategory category, LocalDate reference) {
        CashCutReport report = generate(period, category, reference);
        return new ExportedFile(
                CashCutWorkbookWriter.fileName(report),
                CashCutWorkbookWriter.write(report, LocalDateTime.now()));
    }

    private void accumulate(
            Map<PaymentMethod, BigDecimal> amounts,
            Map<PaymentMethod, Integer> transactions,
            PaymentMethod method,
            BigDecimal amount) {
        amounts.merge(method, amount, BigDecimal::add);
        transactions.merge(method, 1, Integer::sum);
    }

    private BigDecimal money(BigDecimal amount) {
        return amount == null ? null : amount.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal percentage(BigDecimal amount, BigDecimal total) {
        if (total.signum() == 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return amount.multiply(ONE_HUNDRED).divide(total, 2, RoundingMode.HALF_UP);
    }
}
