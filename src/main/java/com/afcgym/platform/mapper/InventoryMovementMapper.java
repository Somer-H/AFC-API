package com.afcgym.platform.mapper;

import com.afcgym.platform.persistance.entities.InventoryMovementEntity;
import com.afcgym.platform.web.dto.response.InventoryMovementResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface InventoryMovementMapper {

    InventoryMovementResponse toResponse(InventoryMovementEntity entity);
}
