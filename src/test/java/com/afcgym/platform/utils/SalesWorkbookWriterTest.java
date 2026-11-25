package com.afcgym.platform.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.afcgym.platform.persistance.entities.enums.PaymentMethod;
import com.afcgym.platform.service.dto.SalesReport;
import com.afcgym.platform.service.dto.SalesReportLine;
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
import org.springframework.web.util.HtmlUtils;

class SalesWorkbookWriterTest {

    @TempDir
    private Path temporaryFolder;

    @Test
    void givenTwoSales_whenWrite_thenTheSheetKeepsTheRangeEveryLineAndTheTotal() throws IOException {
        SalesReport report = new SalesReport(
                LocalDate.of(2026, 11, 1),
                LocalDate.of(2026, 11, 30),
                List.of(
                        new SalesReportLine("V-000002", LocalDateTime.of(2026, 11, 20, 18, 5), PaymentMethod.CARD,
                                "2 x Whey Protein, 1 x Shaker", "Recepción Uno", new BigDecimal("1850.00")),
                        new SalesReportLine("V-000001", LocalDateTime.of(2026, 11, 3, 9, 30), PaymentMethod.CASH,
                                "1 x Agua", "", new BigDecimal("25.00"))),
                new BigDecimal("1875.00"));

        String content = readAllXml(SalesWorkbookWriter.write(report, LocalDateTime.of(2026, 11, 30, 21, 0)));

        assertTrue(content.contains("Historial de ventas"));
        assertTrue(content.contains("01/11/2026"));
        assertTrue(content.contains("30/11/2026 21:00"));
        assertTrue(content.contains("Método de pago"));
        assertTrue(content.contains("Atendió"));
        assertTrue(content.contains("V-000002"));
        assertTrue(content.contains("2 x Whey Protein, 1 x Shaker"));
        assertTrue(content.contains("Recepción Uno"));
        assertTrue(content.contains("Tarjeta"));
        assertTrue(content.contains("Efectivo"));
        assertTrue(content.contains("1875"));
        assertFalse(content.contains("No hay ventas registradas en este periodo"));
    }

    @Test
    void givenNoSales_whenWrite_thenTheSheetSaysThereAreNoSales() throws IOException {
        SalesReport report = new SalesReport(
                LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 1), List.of(), BigDecimal.ZERO);

        String content = readAllXml(SalesWorkbookWriter.write(report, LocalDateTime.of(2026, 11, 1, 21, 0)));

        assertTrue(content.contains("No hay ventas registradas en este periodo"));
        assertTrue(content.contains("Folio"));
    }

    @Test
    void givenARange_whenFileName_thenIncludeBothDates() {
        assertEquals("ventas_2026-11-01_2026-11-30.xlsx",
                SalesWorkbookWriter.fileName(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30)));
    }

    private String readAllXml(byte[] workbook) throws IOException {
        Path file = Files.write(temporaryFolder.resolve("ventas.xlsx"), workbook);
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
        return HtmlUtils.htmlUnescape(content.toString());
    }
}
