package com.afcgym.platform.mapper;

import com.afcgym.platform.persistance.entities.SaleEntity;
import com.afcgym.platform.persistance.entities.SaleItemEntity;
import com.afcgym.platform.web.dto.response.SaleItemResponse;
import com.afcgym.platform.web.dto.response.SaleResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface SaleMapper {

    SaleResponse toResponse(SaleEntity entity);

    SaleItemResponse toItemResponse(SaleItemEntity entity);
}
