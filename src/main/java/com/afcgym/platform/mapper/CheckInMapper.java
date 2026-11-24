package com.afcgym.platform.mapper;

import com.afcgym.platform.persistance.entities.CheckInEntity;
import com.afcgym.platform.web.dto.response.CheckInResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CheckInMapper {

    CheckInResponse toResponse(CheckInEntity entity);
}
