package com.afcgym.platform.utils;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.afcgym.platform.persistance.entities.enums.PaymentMethod;
import com.afcgym.platform.service.dto.CashCutCategory;
import com.afcgym.platform.service.dto.CashCutPeriod;
import com.afcgym.platform.service.dto.CashCutReport;
import com.afcgym.platform.service.dto.PaymentMethodBreakdown;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CashCutWorkbookWriterTest {

    @TempDir
    private Path temporaryFolder;

    @Test
    void givenAStoreOnlyReport_whenWrite_thenTheSheetKeepsTheFiltersAndSkipsTheMembershipRows() throws IOException {
        CashCutReport report = new CashCutReport(
                CashCutPeriod.WEEKLY,
                CashCutCategory.STORE,
                LocalDate.of(2026, 11, 9),
                LocalDate.of(2026, 11, 15),
                null,
                new BigDecimal("150.00"),
                new BigDecimal("150.00"),
                0,
                1,
                List.of(
                        new PaymentMethodBreakdown(PaymentMethod.CARD, new BigDecimal("0.00"), new BigDecimal("0.00"), 0),
                        new PaymentMethodBreakdown(PaymentMethod.SPEI, new BigDecimal("0.00"), new BigDecimal("0.00"), 0),
                        new PaymentMethodBreakdown(
                                PaymentMethod.CASH, new BigDecimal("150.00"), new BigDecimal("100.00"), 1)));

        String content = readAllXml(CashCutWorkbookWriter.write(report, LocalDateTime.of(2026, 11, 17, 18, 30)));

        assertTrue(content.contains("Semanal"));
        assertTrue(content.contains("Tienda"));
        assertTrue(content.contains("09/11/2026"));
        assertTrue(content.contains("15/11/2026"));
        assertTrue(content.contains("17/11/2026 18:30"));
        assertTrue(content.contains("Ingresos por tienda"));
        assertTrue(content.contains("Efectivo"));
        assertTrue(content.contains("Transferencia (SPEI)"));
        assertFalse(content.contains("Ingresos por membresías"));
    }

    private String readAllXml(byte[] workbook) throws IOException {
        Path file = Files.write(temporaryFolder.resolve("corte.xlsx"), workbook);
        StringBuilder content = new StringBuilder();
        try (ZipFile zip = new ZipFile(file.toFile())) {
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (entry.getName().endsWith(".xml")) {
                    try (InputStream input = zip.getInputStream(entry)) {
                        content.append(new String(input.readAllBytes(), StandardCharsets.UTF_8));
                    }
                }
            }
        }
        return content.toString();
    }
}
