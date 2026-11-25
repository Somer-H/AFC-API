package com.afcgym.platform.service.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record SalesReport(
        LocalDate from,
        LocalDate to,
        List<SalesReportLine> lines,
        BigDecimal total
) {}
