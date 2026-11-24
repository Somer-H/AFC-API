package com.afcgym.platform.service.impl;

import com.afcgym.platform.exception.BadRequestException;
import com.afcgym.platform.persistance.entities.AuditLogEntity;
import com.afcgym.platform.persistance.repositories.IAuditLogRepository;
import com.afcgym.platform.persistance.repositories.specifications.AuditLogSpecifications;
import com.afcgym.platform.service.IAuditService;
import com.afcgym.platform.service.dto.AuditEntry;
import com.afcgym.platform.service.dto.AuditLogFilter;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuditServiceImpl implements IAuditService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final int MAX_TEXT_LENGTH = 300;
    private static final int MAX_ACTION_LENGTH = 100;
    private static final int MAX_TARGET_LENGTH = 50;

    private final IAuditLogRepository auditLogRepository;

    @Override
    @Transactional
    public AuditLogEntity record(AuditEntry entry) {
        return auditLogRepository.save(AuditLogEntity.builder()
                .occurredAt(Instant.now())
                .actorUserId(entry.actorUserId())
                .actorRole(entry.actorRole())
                .action(truncate(entry.action(), MAX_ACTION_LENGTH))
                .httpMethod(entry.httpMethod())
                .path(truncate(entry.path(), MAX_TEXT_LENGTH))
                .targetId(truncate(entry.targetId(), MAX_TARGET_LENGTH))
                .outcome(entry.outcome())
                .responseStatus(entry.responseStatus())
                .build());
    }

    @Override
    public Page<AuditLogEntity> search(AuditLogFilter filter, int page, int size) {
        if (page < 0) {
            throw new BadRequestException("La página debe ser 0 o mayor");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new BadRequestException("El tamaño de página debe estar entre 1 y " + MAX_PAGE_SIZE);
        }
        if (filter.from() != null && filter.to() != null && filter.to().isBefore(filter.from())) {
            throw new BadRequestException("La fecha final no puede ser anterior a la inicial");
        }
        Specification<@NonNull AuditLogEntity> specification = AuditLogSpecifications.all();
        if (filter.actorUserId() != null) {
            specification = specification.and(AuditLogSpecifications.actorEquals(filter.actorUserId()));
        }
        if (filter.action() != null && !filter.action().isBlank()) {
            specification = specification.and(AuditLogSpecifications.actionEquals(filter.action().strip()));
        }
        if (filter.outcome() != null) {
            specification = specification.and(AuditLogSpecifications.outcomeEquals(filter.outcome()));
        }
        if (filter.from() != null) {
            specification = specification.and(AuditLogSpecifications.occurredFrom(startOf(filter.from())));
        }
        if (filter.to() != null) {
            specification = specification.and(AuditLogSpecifications.occurredBefore(startOf(filter.to().plusDays(1))));
        }
        return auditLogRepository.findAll(specification,
                PageRequest.of(page, size, Sort.by(Sort.Order.desc("occurredAt"), Sort.Order.desc("id"))));
    }

    private Instant startOf(LocalDate date) {
        return date.atStartOfDay(ZoneId.systemDefault()).toInstant();
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
