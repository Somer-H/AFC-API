package com.afcgym.platform.service;

import com.afcgym.platform.persistance.entities.RoutineEntity;
import com.afcgym.platform.service.dto.RoutineCommand;
import com.afcgym.platform.service.dto.VideoUpload;
import java.time.DayOfWeek;
import java.util.List;

public interface IRoutineService {

    RoutineEntity create(RoutineCommand command);

    List<RoutineEntity> list();

    RoutineEntity getById(Long id);

    RoutineEntity update(Long id, RoutineCommand command);

    void delete(Long id);

    RoutineEntity getForDay(DayOfWeek dayOfWeek);

    RoutineEntity attachVideoFile(Long routineId, Long exerciseId, VideoUpload upload);

    RoutineEntity removeVideo(Long routineId, Long exerciseId);
}
