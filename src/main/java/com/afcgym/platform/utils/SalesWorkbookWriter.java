package com.afcgym.platform.utils;

import com.afcgym.platform.service.dto.SalesReport;
import com.afcgym.platform.service.dto.SalesReportLine;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.dhatim.fastexcel.Workbook;
import org.dhatim.fastexcel.Worksheet;

public final class SalesWorkbookWriter {

    private static final String APPLICATION_NAME = "AFC Gym";
    private static final String APPLICATION_VERSION = "1.0";
    private static final String SHEET_NAME = "Ventas";
    private static final String TITLE = "Historial de ventas";
    private static final String MONEY_FORMAT = "\"$\"#,##0.00";
    private static final String DATE_TIME_CELL_FORMAT = "dd/mm/yyyy hh:mm";
    private static final String HEADER_FILL = "F2F2F2";
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final String[] HEADERS = {"Folio", "Fecha y hora", "Método de pago", "Productos", "Atendió", "Total"};

    private SalesWorkbookWriter() {
    }

    public static String fileName(LocalDate from, LocalDate to) {
        return "ventas_" + from + "_" + to + ".xlsx";
    }

    public static byte[] write(SalesReport report, LocalDateTime generatedAt) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (Workbook workbook = new Workbook(output, APPLICATION_NAME, APPLICATION_VERSION)) {
            Worksheet sheet = workbook.newWorksheet(SHEET_NAME);
            sheet.width(0, 16);
            sheet.width(1, 18);
            sheet.width(2, 22);
            sheet.width(3, 50);
            sheet.width(4, 26);
            sheet.width(5, 14);

            sheet.value(0, 0, TITLE);
            sheet.style(0, 0).bold().fontSize(14).set();

            int row = 2;
            row = writeText(sheet, row, "Desde", report.from().format(DATE_FORMAT));
            row = writeText(sheet, row, "Hasta", report.to().format(DATE_FORMAT));
            row = writeText(sheet, row, "Generado", generatedAt.format(DATE_TIME_FORMAT));
            sheet.value(row, 0, "Ventas");
            sheet.value(row, 1, report.lines().size());
            row++;
            sheet.value(row, 0, "Total vendido");
            sheet.value(row, 1, report.total());
            sheet.style(row, 0).bold().set();
            sheet.style(row, 1).bold().format(MONEY_FORMAT).set();
            row += 2;

            for (int column = 0; column < HEADERS.length; column++) {
                sheet.value(row, column, HEADERS[column]);
                sheet.style(row, column).bold().fillColor(HEADER_FILL).set();
            }
            row++;
            if (report.lines().isEmpty()) {
                sheet.value(row, 0, "No hay ventas registradas en este periodo");
            } else {
                writeLines(sheet, row, report);
            }
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
        return output.toByteArray();
    }

    private static void writeLines(Worksheet sheet, int firstRow, SalesReport report) {
        int row = firstRow;
        for (SalesReportLine line : report.lines()) {
            sheet.value(row, 0, line.folio());
            sheet.value(row, 1, line.soldAt());
            sheet.style(row, 1).format(DATE_TIME_CELL_FORMAT).set();
            sheet.value(row, 2, PaymentMethodLabels.spanish(line.paymentMethod()));
            sheet.value(row, 3, line.products());
            sheet.value(row, 4, line.cashier());
            sheet.value(row, 5, line.total());
            sheet.style(row, 5).format(MONEY_FORMAT).set();
            row++;
        }
        sheet.value(row, 4, "Total");
        sheet.value(row, 5, report.total());
        sheet.style(row, 4).bold().set();
        sheet.style(row, 5).bold().format(MONEY_FORMAT).set();
    }

    private static int writeText(Worksheet sheet, int row, String label, String value) {
        sheet.value(row, 0, label);
        sheet.value(row, 1, value);
        return row + 1;
    }
}
