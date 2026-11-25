package com.afcgym.platform.mapper;

import com.afcgym.platform.persistance.entities.AuditLogEntity;
import com.afcgym.platform.web.dto.response.AuditLogPageResponse;
import com.afcgym.platform.web.dto.response.AuditLogResponse;
import org.mapstruct.Mapper;
import org.springframework.data.domain.Page;

@Mapper(componentModel = "spring")
public interface AuditLogMapper {

    AuditLogResponse toResponse(AuditLogEntity entity);

    default AuditLogPageResponse toPageResponse(Page<AuditLogEntity> page) {
        return new AuditLogPageResponse(page.getContent().stream().map(this::toResponse).toList(),
                page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }
}
