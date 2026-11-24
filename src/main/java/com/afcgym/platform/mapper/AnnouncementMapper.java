package com.afcgym.platform.mapper;

import com.afcgym.platform.persistance.entities.AnnouncementEntity;
import com.afcgym.platform.web.dto.response.AnnouncementResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AnnouncementMapper {

    @Mapping(target = "current", expression = "java(entity.isCurrentOn(java.time.LocalDate.now()))")
    AnnouncementResponse toResponse(AnnouncementEntity entity);
}
