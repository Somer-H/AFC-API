package com.afcgym.platform.mapper;

import com.afcgym.platform.persistance.entities.CategoryEntity;
import com.afcgym.platform.web.dto.response.CategoryResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CategoryMapper {

    CategoryResponse toResponse(CategoryEntity entity);
}
