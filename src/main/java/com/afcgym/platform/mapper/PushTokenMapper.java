package com.afcgym.platform.mapper;

import com.afcgym.platform.persistance.entities.PushTokenEntity;
import com.afcgym.platform.web.dto.response.PushTokenResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PushTokenMapper {

    PushTokenResponse toResponse(PushTokenEntity entity);
}
