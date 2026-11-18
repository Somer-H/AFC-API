package com.afcgym.platform.mapper;

import com.afcgym.platform.persistance.entities.BookingEntity;
import com.afcgym.platform.web.dto.response.BookingResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface BookingMapper {

    @Mapping(target = "scheduleBlockId", source = "scheduleBlock.id")
    @Mapping(target = "dayOfWeek", source = "scheduleBlock.dayOfWeek")
    @Mapping(target = "startTime", source = "scheduleBlock.startTime")
    @Mapping(target = "endTime", source = "scheduleBlock.endTime")
    BookingResponse toResponse(BookingEntity entity);
}
