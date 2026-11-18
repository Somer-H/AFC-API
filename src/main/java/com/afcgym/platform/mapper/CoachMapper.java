package com.afcgym.platform.mapper;

import com.afcgym.platform.persistance.entities.CoachEntity;
import com.afcgym.platform.web.dto.response.CoachResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CoachMapper {

    CoachResponse toResponse(CoachEntity entity);
}
