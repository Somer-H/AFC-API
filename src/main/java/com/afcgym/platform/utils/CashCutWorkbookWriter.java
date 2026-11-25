package com.afcgym.platform.utils;

import com.afcgym.platform.service.dto.CashCutCategory;
import com.afcgym.platform.service.dto.CashCutPeriod;
import com.afcgym.platform.service.dto.CashCutReport;
import com.afcgym.platform.service.dto.PaymentMethodBreakdown;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.dhatim.fastexcel.Workbook;
import org.dhatim.fastexcel.Worksheet;

public final class CashCutWorkbookWriter {

    private static final String APPLICATION_NAME = "AFC Gym";
    private static final String APPLICATION_VERSION = "1.0";
    private static final String SHEET_NAME = "Corte de caja";
    private static final String MONEY_FORMAT = "\"$\"#,##0.00";
    private static final String PERCENT_FORMAT = "0.00\"%\"";
    private static final String HEADER_FILL = "F2F2F2";
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private CashCutWorkbookWriter() {
    }

    public static String fileName(CashCutReport report) {
        return "corte-de-caja_" + periodLabel(report.period()).toLowerCase()
                + "_" + categoryFileLabel(report.category())
                + "_" + report.from() + "_" + report.to() + ".xlsx";
    }

    public static byte[] write(CashCutReport report, LocalDateTime generatedAt) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (Workbook workbook = new Workbook(output, APPLICATION_NAME, APPLICATION_VERSION)) {
            Worksheet sheet = workbook.newWorksheet(SHEET_NAME);
            sheet.width(0, 30);
            sheet.width(1, 18);
            sheet.width(2, 14);
            sheet.width(3, 14);

            sheet.value(0, 0, SHEET_NAME);
            sheet.style(0, 0).bold().fontSize(14).set();

            int row = 2;
            row = writeText(sheet, row, "Periodo", periodLabel(report.period()));
            row = writeText(sheet, row, "Categoría", categoryLabel(report.category()));
            row = writeText(sheet, row, "Desde", report.from().format(DATE_FORMAT));
            row = writeText(sheet, row, "Hasta", report.to().format(DATE_FORMAT));
            row = writeText(sheet, row, "Generado", generatedAt.format(DATE_TIME_FORMAT));
            row++;

            if (report.membershipIncome() != null) {
                row = writeMoney(sheet, row, "Ingresos por membresías", report.membershipIncome());
                row = writeCount(sheet, row, "Pagos de membresía", report.membershipPayments());
            }
            if (report.storeIncome() != null) {
                row = writeMoney(sheet, row, "Ingresos por tienda", report.storeIncome());
                row = writeCount(sheet, row, "Ventas de tienda", report.storeSales());
            }
            sheet.style(row, 0).bold().set();
            row = writeMoney(sheet, row, "Total de ingresos", report.totalIncome());
            row++;

            String[] headers = {"Método de pago", "Monto", "Porcentaje", "Operaciones"};
            for (int column = 0; column < headers.length; column++) {
                sheet.value(row, column, headers[column]);
                sheet.style(row, column).bold().fillColor(HEADER_FILL).set();
            }
            row++;
            for (PaymentMethodBreakdown breakdown : report.paymentMethods()) {
                sheet.value(row, 0, PaymentMethodLabels.spanish(breakdown.method()));
                sheet.value(row, 1, breakdown.amount());
                sheet.style(row, 1).format(MONEY_FORMAT).set();
                sheet.value(row, 2, breakdown.percentage());
                sheet.style(row, 2).format(PERCENT_FORMAT).set();
                sheet.value(row, 3, breakdown.transactions());
                row++;
            }
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
        return output.toByteArray();
    }

    private static int writeText(Worksheet sheet, int row, String label, String value) {
        sheet.value(row, 0, label);
        sheet.value(row, 1, value);
        return row + 1;
    }

    private static int writeMoney(Worksheet sheet, int row, String label, BigDecimal value) {
        sheet.value(row, 0, label);
        sheet.value(row, 1, value);
        sheet.style(row, 1).format(MONEY_FORMAT).set();
        return row + 1;
    }

    private static int writeCount(Worksheet sheet, int row, String label, int value) {
        sheet.value(row, 0, label);
        sheet.value(row, 1, value);
        return row + 1;
    }

    private static String periodLabel(CashCutPeriod period) {
        return switch (period) {
            case DAILY -> "Diario";
            case WEEKLY -> "Semanal";
            case MONTHLY -> "Mensual";
        };
    }

    private static String categoryLabel(CashCutCategory category) {
        return switch (category) {
            case MEMBERSHIPS -> "Membresías";
            case STORE -> "Tienda";
            case ALL -> "Membresías y tienda";
        };
    }

    private static String categoryFileLabel(CashCutCategory category) {
        return switch (category) {
            case MEMBERSHIPS -> "membresias";
            case STORE -> "tienda";
            case ALL -> "todo";
        };
    }
}
