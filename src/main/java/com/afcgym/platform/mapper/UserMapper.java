package com.afcgym.platform.mapper;

import com.afcgym.platform.persistance.entities.UserEntity;
import com.afcgym.platform.web.dto.response.UserResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserResponse toResponse(UserEntity entity);
}
