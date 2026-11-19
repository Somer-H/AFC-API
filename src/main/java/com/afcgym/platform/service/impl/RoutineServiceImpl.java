package com.afcgym.platform.service.impl;

import com.afcgym.platform.exception.BadRequestException;
import com.afcgym.platform.exception.ConflictException;
import com.afcgym.platform.exception.NotFoundException;
import com.afcgym.platform.persistance.entities.CoachEntity;
import com.afcgym.platform.persistance.entities.ExerciseEntity;
import com.afcgym.platform.persistance.entities.RoutineEntity;
import com.afcgym.platform.persistance.repositories.ICoachRepository;
import com.afcgym.platform.persistance.repositories.IRoutineRepository;
import com.afcgym.platform.service.IRoutineService;
import com.afcgym.platform.service.IVideoStorageService;
import com.afcgym.platform.service.dto.ExerciseCommand;
import com.afcgym.platform.service.dto.RoutineCommand;
import com.afcgym.platform.service.dto.VideoUpload;
import com.afcgym.platform.utils.SpanishDateFormatter;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RoutineServiceImpl implements IRoutineService {

    private static final int MAX_EXERCISES = 8;
    private static final String ROUTINE_NOT_FOUND = "No se encontró la rutina solicitada";

    private final IRoutineRepository routineRepository;
    private final ICoachRepository coachRepository;
    private final IVideoStorageService videoStorageService;

    @Override
    @Transactional
    public RoutineEntity create(RoutineCommand command) {
        validate(command);
        ensureDayIsFree(command.dayOfWeek());
        CoachEntity coach = resolveCoach(command.coachId());
        List<ExerciseEntity> exercises = new ArrayList<>();
        for (int index = 0; index < command.exercises().size(); index++) {
            exercises.add(newExercise(index, command.exercises().get(index)));
        }
        return routineRepository.save(RoutineEntity.builder()
                .dayOfWeek(command.dayOfWeek())
                .title(command.title().trim())
                .description(command.description())
                .coach(coach)
                .createdAt(Instant.now())
                .exercises(exercises)
                .build());
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoutineEntity> list() {
        return routineRepository.findAll().stream()
                .sorted(Comparator.comparing(RoutineEntity::getDayOfWeek))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public RoutineEntity getById(Long id) {
        return routineRepository.findWithDetailsById(id)
                .orElseThrow(() -> new NotFoundException(ROUTINE_NOT_FOUND));
    }

    @Override
    @Transactional
    public RoutineEntity update(Long id, RoutineCommand command) {
        validate(command);
        RoutineEntity routine = getById(id);
        if (command.dayOfWeek() != routine.getDayOfWeek()) {
            ensureDayIsFree(command.dayOfWeek());
        }
        CoachEntity coach = resolveCoach(command.coachId());

        Map<Long, ExerciseEntity> current = routine.getExercises().stream()
                .collect(Collectors.toMap(ExerciseEntity::getId, Function.identity()));
        Set<Long> kept = new HashSet<>();
        List<ExerciseEntity> exercises = new ArrayList<>();
        for (int index = 0; index < command.exercises().size(); index++) {
            ExerciseCommand exerciseCommand = command.exercises().get(index);
            if (exerciseCommand.id() == null) {
                exercises.add(newExercise(index, exerciseCommand));
                continue;
            }
            ExerciseEntity exercise = current.get(exerciseCommand.id());
            if (exercise == null) {
                throw new BadRequestException(
                        "El ejercicio " + exerciseCommand.id() + " no pertenece a esta rutina");
            }
            if (!kept.add(exercise.getId())) {
                throw new BadRequestException("El ejercicio " + exercise.getId() + " viene repetido");
            }
            exercise.updateDetails(index, exerciseCommand.name().trim(), exerciseCommand.sets(),
                    exerciseCommand.reps().trim(), exerciseCommand.restSeconds());
            applyVideoUrl(exercise, exerciseCommand.videoUrl());
            exercises.add(exercise);
        }
        current.values().stream()
                .filter(exercise -> !kept.contains(exercise.getId()))
                .forEach(exercise -> videoStorageService.deleteAfterCommit(exercise.getVideoFile()));

        routine.updateDetails(command.dayOfWeek(), command.title().trim(), command.description(), coach);
        routine.replaceExercises(exercises);
        return routine;
    }

    @Override
    @Transactional
    public void delete(Long id) {
        RoutineEntity routine = getById(id);
        routine.getExercises().forEach(exercise -> videoStorageService.deleteAfterCommit(exercise.getVideoFile()));
        routineRepository.delete(routine);
    }

    @Override
    @Transactional(readOnly = true)
    public RoutineEntity getForDay(DayOfWeek dayOfWeek) {
        DayOfWeek day = dayOfWeek != null ? dayOfWeek : LocalDate.now().getDayOfWeek();
        return routineRepository.findByDayOfWeek(day)
                .orElseThrow(() -> new NotFoundException(
                        "No hay rutina asignada para el " + SpanishDateFormatter.dayName(day)));
    }

    @Override
    @Transactional
    public RoutineEntity attachVideoFile(Long routineId, Long exerciseId, VideoUpload upload) {
        RoutineEntity routine = getById(routineId);
        ExerciseEntity exercise = findExercise(routine, exerciseId);
        String fileName = videoStorageService.store(upload);
        videoStorageService.deleteAfterCommit(exercise.useVideoFile(fileName));
        return routine;
    }

    @Override
    @Transactional
    public RoutineEntity removeVideo(Long routineId, Long exerciseId) {
        RoutineEntity routine = getById(routineId);
        ExerciseEntity exercise = findExercise(routine, exerciseId);
        videoStorageService.deleteAfterCommit(exercise.useVideoUrl(null));
        return routine;
    }

    private void validate(RoutineCommand command) {
        if (command.coachId() == null) {
            throw new BadRequestException("El coach es obligatorio");
        }
        if (command.exercises() == null || command.exercises().isEmpty()) {
            throw new BadRequestException("La rutina necesita al menos un ejercicio");
        }
        if (command.exercises().size() > MAX_EXERCISES) {
            throw new BadRequestException("Una rutina admite como máximo " + MAX_EXERCISES + " ejercicios");
        }
        for (ExerciseCommand exercise : command.exercises()) {
            if (exercise.sets() <= 0) {
                throw new BadRequestException("Las series de cada ejercicio deben ser mayores a cero");
            }
            if (exercise.restSeconds() != null && exercise.restSeconds() < 0) {
                throw new BadRequestException("El descanso no puede ser negativo");
            }
            String videoUrl = normalizeUrl(exercise.videoUrl());
            if (videoUrl != null && !videoUrl.startsWith("https://") && !videoUrl.startsWith("http://")) {
                throw new BadRequestException("El video debe ser una URL que empiece con http:// o https://");
            }
        }
    }

    private String normalizeUrl(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private void ensureDayIsFree(DayOfWeek dayOfWeek) {
        if (routineRepository.existsByDayOfWeek(dayOfWeek)) {
            throw new ConflictException("Ya hay una rutina para el " + SpanishDateFormatter.dayName(dayOfWeek)
                    + "; edítala o elimínala primero");
        }
    }

    private CoachEntity resolveCoach(Long coachId) {
        return coachRepository.findById(coachId)
                .orElseThrow(() -> new NotFoundException("No se encontró el coach solicitado"));
    }

    private ExerciseEntity newExercise(int index, ExerciseCommand command) {
        return ExerciseEntity.builder()
                .sortOrder(index)
                .name(command.name().trim())
                .sets(command.sets())
                .reps(command.reps().trim())
                .restSeconds(command.restSeconds())
                .videoUrl(normalizeUrl(command.videoUrl()))
                .build();
    }

    private void applyVideoUrl(ExerciseEntity exercise, String requestedUrl) {
        String videoUrl = normalizeUrl(requestedUrl);
        if (videoUrl != null) {
            videoStorageService.deleteAfterCommit(exercise.useVideoUrl(videoUrl));
            return;
        }
        if (exercise.getVideoFile() == null) {
            exercise.useVideoUrl(null);
        }
    }

    private ExerciseEntity findExercise(RoutineEntity routine, Long exerciseId) {
        return routine.getExercises().stream()
                .filter(exercise -> exercise.getId().equals(exerciseId))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("No se encontró el ejercicio en esta rutina"));
    }
}
