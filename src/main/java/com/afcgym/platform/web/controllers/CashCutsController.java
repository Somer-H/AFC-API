package com.afcgym.platform.web.controllers;

import com.afcgym.platform.mapper.CashCutMapper;
import com.afcgym.platform.service.ICashCutService;
import com.afcgym.platform.service.dto.CashCutCategory;
import com.afcgym.platform.service.dto.CashCutExport;
import com.afcgym.platform.service.dto.CashCutPeriod;
import com.afcgym.platform.web.dto.response.CashCutResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/cash-cuts")
@RequiredArgsConstructor
@Tag(name = "Cash cuts")
public class CashCutsController {

    private static final MediaType XLSX =
            MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private final ICashCutService cashCutService;
    private final CashCutMapper cashCutMapper;

    @GetMapping
    @Operation(summary = "Income report for the day, week (Monday to Sunday) or month that contains the given date")
    public CashCutResponse generate(
            @RequestParam CashCutPeriod period,
            @RequestParam CashCutCategory category,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return cashCutMapper.toResponse(cashCutService.generate(period, category, date));
    }

    @GetMapping("/export")
    @Operation(summary = "Download the same income report as an Excel file")
    public ResponseEntity<byte[]> export(
            @RequestParam CashCutPeriod period,
            @RequestParam CashCutCategory category,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        CashCutExport export = cashCutService.export(period, category, date);
        return ResponseEntity.ok()
                .contentType(XLSX)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(export.fileName()).build().toString())
                .body(export.content());
    }
}
