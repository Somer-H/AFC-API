package com.afcgym.platform.mapper;

import com.afcgym.platform.persistance.entities.ScheduleBlockEntity;
import com.afcgym.platform.persistance.entities.enums.Shift;
import com.afcgym.platform.service.dto.BlockOccupancy;
import com.afcgym.platform.service.dto.DayOccupancy;
import com.afcgym.platform.web.dto.response.BlockOccupancyResponse;
import com.afcgym.platform.web.dto.response.DayOccupancyResponse;
import com.afcgym.platform.web.dto.response.ScheduleBlockResponse;
import java.time.LocalTime;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring", uses = CoachMapper.class)
public interface ScheduleBlockMapper {

    @Mapping(target = "shift", source = "startTime", qualifiedByName = "toShift")
    ScheduleBlockResponse toResponse(ScheduleBlockEntity entity);

    BlockOccupancyResponse toOccupancyResponse(BlockOccupancy occupancy);

    DayOccupancyResponse toDayOccupancyResponse(DayOccupancy occupancy);

    @Named("toShift")
    default String toShift(LocalTime startTime) {
        return Shift.fromStartTime(startTime).name();
    }
}
