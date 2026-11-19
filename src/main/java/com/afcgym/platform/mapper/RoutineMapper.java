package com.afcgym.platform.mapper;

import com.afcgym.platform.persistance.entities.ExerciseEntity;
import com.afcgym.platform.persistance.entities.RoutineEntity;
import com.afcgym.platform.web.dto.response.ExerciseResponse;
import com.afcgym.platform.web.dto.response.RoutineResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = CoachMapper.class)
public interface RoutineMapper {

    String VIDEO_FILE_PATH = "/v1/exercise-videos/";

    RoutineResponse toResponse(RoutineEntity entity);

    @Mapping(target = "videoType", expression = "java(videoType(entity))")
    @Mapping(target = "videoUrl", expression = "java(playableVideoUrl(entity))")
    ExerciseResponse toExerciseResponse(ExerciseEntity entity);

    default String videoType(ExerciseEntity entity) {
        if (entity.getVideoFile() != null) {
            return "FILE";
        }
        return entity.getVideoUrl() != null ? "URL" : null;
    }

    default String playableVideoUrl(ExerciseEntity entity) {
        if (entity.getVideoFile() != null) {
            return VIDEO_FILE_PATH + entity.getVideoFile();
        }
        return entity.getVideoUrl();
    }
}
