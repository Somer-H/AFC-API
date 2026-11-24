package com.afcgym.platform.service;

import com.afcgym.platform.persistance.entities.AuditLogEntity;
import com.afcgym.platform.service.dto.AuditEntry;
import com.afcgym.platform.service.dto.AuditLogFilter;
import org.springframework.data.domain.Page;

public interface IAuditService {

    AuditLogEntity record(AuditEntry entry);

    Page<AuditLogEntity> search(AuditLogFilter filter, int page, int size);
}
