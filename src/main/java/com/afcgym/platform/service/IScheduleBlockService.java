package com.afcgym.platform.service;

import com.afcgym.platform.persistance.entities.ScheduleBlockEntity;
import com.afcgym.platform.service.dto.DayOccupancy;
import com.afcgym.platform.service.dto.ScheduleBlockCommand;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

public interface IScheduleBlockService {

    ScheduleBlockEntity create(ScheduleBlockCommand command);

    ScheduleBlockEntity getById(Long id);

    List<ScheduleBlockEntity> list(DayOfWeek dayOfWeek);

    ScheduleBlockEntity update(Long id, ScheduleBlockCommand command);

    ScheduleBlockEntity activate(Long id);

    ScheduleBlockEntity deactivate(Long id);

    DayOccupancy occupancy(LocalDate date, DayOfWeek dayOfWeek);
}
