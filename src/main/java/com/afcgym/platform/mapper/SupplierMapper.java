package com.afcgym.platform.mapper;

import com.afcgym.platform.persistance.entities.SupplierEntity;
import com.afcgym.platform.web.dto.response.SupplierResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface SupplierMapper {

    SupplierResponse toResponse(SupplierEntity entity);
}
