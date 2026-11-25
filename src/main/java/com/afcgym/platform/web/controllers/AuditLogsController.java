package com.afcgym.platform.web.controllers;

import com.afcgym.platform.mapper.AuditLogMapper;
import com.afcgym.platform.persistance.entities.enums.AuditOutcome;
import com.afcgym.platform.service.IAuditService;
import com.afcgym.platform.service.dto.AuditLogFilter;
import com.afcgym.platform.web.dto.response.AuditLogPageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/audit-logs")
@RequiredArgsConstructor
@Tag(name = "Audit log")
public class AuditLogsController {

    private final IAuditService auditService;
    private final AuditLogMapper auditLogMapper;

    @GetMapping
    @Operation(summary = "Read-only history of admin changes and denied access attempts, newest first")
    public AuditLogPageResponse search(
            @RequestParam(required = false) Long actorUserId,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) AuditOutcome outcome,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return auditLogMapper.toPageResponse(auditService.search(
                new AuditLogFilter(actorUserId, action, outcome, from, to), page, size));
    }
}
