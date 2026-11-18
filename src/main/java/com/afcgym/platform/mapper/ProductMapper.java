package com.afcgym.platform.mapper;

import com.afcgym.platform.persistance.entities.ProductEntity;
import com.afcgym.platform.web.dto.response.ProductResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ProductMapper {

    @Mapping(target = "lowStock", expression = "java(entity.getStock() <= entity.getMinStock())")
    ProductResponse toResponse(ProductEntity entity);
}
