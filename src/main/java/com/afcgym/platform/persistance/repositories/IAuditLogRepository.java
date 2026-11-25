package com.afcgym.platform.persistance.repositories;

import com.afcgym.platform.persistance.entities.AuditLogEntity;
import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.repository.Repository;

public interface IAuditLogRepository extends Repository<@NonNull AuditLogEntity, @NonNull Long> {

    AuditLogEntity save(AuditLogEntity entity);

    Page<AuditLogEntity> findAll(Specification<AuditLogEntity> specification, Pageable pageable);
}
