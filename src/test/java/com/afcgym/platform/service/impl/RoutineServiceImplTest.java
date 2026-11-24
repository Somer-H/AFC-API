package com.afcgym.platform.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.afcgym.platform.exception.BadRequestException;
import com.afcgym.platform.exception.ConflictException;
import com.afcgym.platform.exception.NotFoundException;
import com.afcgym.platform.persistance.entities.CoachEntity;
import com.afcgym.platform.persistance.entities.ExerciseEntity;
import com.afcgym.platform.persistance.entities.RoutineEntity;
import com.afcgym.platform.persistance.repositories.ICoachRepository;
import com.afcgym.platform.persistance.repositories.IRoutineRepository;
import com.afcgym.platform.service.IVideoStorageService;
import com.afcgym.platform.service.dto.ExerciseCommand;
import com.afcgym.platform.service.dto.RoutineCommand;
import com.afcgym.platform.service.dto.VideoUpload;
import java.io.ByteArrayInputStream;
import java.time.DayOfWeek;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RoutineServiceImplTest {

    @Mock
    private IRoutineRepository routineRepository;

    @Mock
    private ICoachRepository coachRepository;

    @Mock
    private IVideoStorageService videoStorageService;

    @InjectMocks
    private RoutineServiceImpl routineService;

    private CoachEntity coach;

    @BeforeEach
    void init() {
        coach = CoachEntity.builder().id(3L).name("Alex Rivera").createdAt(Instant.now()).build();
    }

    @Test
    void givenAFreeDay_whenCreate_thenSaveTheExercisesInTheGivenOrder() {
        when(routineRepository.existsByDayOfWeek(DayOfWeek.MONDAY)).thenReturn(false);
        when(coachRepository.findById(3L)).thenReturn(Optional.of(coach));
        when(routineRepository.save(any(RoutineEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RoutineEntity result = routineService.create(command(DayOfWeek.MONDAY,
                exercise(null, "Sentadilla", "https://youtu.be/abc"), exercise(null, "Press banca", null)));

        assertEquals(coach, result.getCoach());
        assertEquals(2, result.getExercises().size());
        assertEquals("Sentadilla", result.getExercises().get(0).getName());
        assertEquals(0, result.getExercises().get(0).getSortOrder());
        assertEquals("https://youtu.be/abc", result.getExercises().get(0).getVideoUrl());
        assertEquals(1, result.getExercises().get(1).getSortOrder());
    }

    @Test
    void givenADayThatAlreadyHasARoutine_whenCreate_thenThrowConflictWithAClearMessage() {
        when(routineRepository.existsByDayOfWeek(DayOfWeek.MONDAY)).thenReturn(true);

        ConflictException exception = assertThrows(ConflictException.class,
                () -> routineService.create(command(DayOfWeek.MONDAY, exercise(null, "Sentadilla", null))));

        assertEquals("Ya hay una rutina para el lunes; edítala o elimínala primero", exception.getMessage());
        verify(routineRepository, never()).save(any());
    }

    @Test
    void givenNineExercises_whenCreate_thenThrowBadRequestException() {
        ExerciseCommand[] nine = IntStream.range(0, 9)
                .mapToObj(index -> exercise(null, "Ejercicio " + index, null))
                .toArray(ExerciseCommand[]::new);

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> routineService.create(command(DayOfWeek.MONDAY, nine)));

        assertEquals("Una rutina admite como máximo 8 ejercicios", exception.getMessage());
    }

    @Test
    void givenNoExercises_whenCreate_thenThrowBadRequestException() {
        assertThrows(BadRequestException.class, () -> routineService.create(command(DayOfWeek.MONDAY)));
    }

    @Test
    void givenAVideoThatIsNotAnHttpUrl_whenCreate_thenThrowBadRequestException() {
        assertThrows(BadRequestException.class, () -> routineService.create(
                command(DayOfWeek.MONDAY, exercise(null, "Sentadilla", "ftp://videos/sentadilla.mp4"))));
    }

    @Test
    void givenAnUnknownCoach_whenCreate_thenThrowNotFoundException() {
        when(routineRepository.existsByDayOfWeek(DayOfWeek.MONDAY)).thenReturn(false);
        when(coachRepository.findById(3L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> routineService.create(command(DayOfWeek.MONDAY, exercise(null, "Sentadilla", null))));
    }

    @Test
    void givenExistingExercises_whenUpdate_thenKeepThoseSentByIdAddNewOnesAndDeleteTheRestWithTheirFiles() {
        ExerciseEntity kept = storedExercise(10L, 0, "Sentadilla", null, "aaaa.mp4");
        ExerciseEntity removed = storedExercise(11L, 1, "Remo", null, "bbbb.mp4");
        RoutineEntity routine = routine(DayOfWeek.MONDAY, kept, removed);
        when(routineRepository.findWithDetailsById(1L)).thenReturn(Optional.of(routine));
        when(coachRepository.findById(3L)).thenReturn(Optional.of(coach));

        RoutineEntity result = routineService.update(1L, command(DayOfWeek.MONDAY,
                exercise(null, "Zancadas", null), exercise(10L, "Sentadilla frontal", null)));

        assertEquals(2, result.getExercises().size());
        assertEquals("Zancadas", result.getExercises().get(0).getName());
        assertSame(kept, result.getExercises().get(1));
        assertEquals("Sentadilla frontal", kept.getName());
        assertEquals(1, kept.getSortOrder());
        assertEquals("aaaa.mp4", kept.getVideoFile());
        verify(videoStorageService).deleteAfterCommit("bbbb.mp4");
    }

    @Test
    void givenAnExerciseWithAFile_whenUpdateWithAVideoUrl_thenTheUrlReplacesTheFile() {
        ExerciseEntity exercise = storedExercise(10L, 0, "Sentadilla", null, "aaaa.mp4");
        when(routineRepository.findWithDetailsById(1L)).thenReturn(Optional.of(routine(DayOfWeek.MONDAY, exercise)));
        when(coachRepository.findById(3L)).thenReturn(Optional.of(coach));

        routineService.update(1L, command(DayOfWeek.MONDAY, exercise(10L, "Sentadilla", "https://youtu.be/xyz")));

        assertEquals("https://youtu.be/xyz", exercise.getVideoUrl());
        assertNull(exercise.getVideoFile());
        verify(videoStorageService).deleteAfterCommit("aaaa.mp4");
    }

    @Test
    void givenAnExerciseOfAnotherRoutine_whenUpdate_thenThrowBadRequestException() {
        when(routineRepository.findWithDetailsById(1L))
                .thenReturn(Optional.of(routine(DayOfWeek.MONDAY, storedExercise(10L, 0, "Sentadilla", null, null))));
        when(coachRepository.findById(3L)).thenReturn(Optional.of(coach));

        assertThrows(BadRequestException.class, () -> routineService.update(
                1L, command(DayOfWeek.MONDAY, exercise(99L, "Ajeno", null))));
    }

    @Test
    void givenADayTakenByAnotherRoutine_whenMoveTheRoutineThere_thenThrowConflictException() {
        when(routineRepository.findWithDetailsById(1L))
                .thenReturn(Optional.of(routine(DayOfWeek.MONDAY, storedExercise(10L, 0, "Sentadilla", null, null))));
        when(routineRepository.existsByDayOfWeek(DayOfWeek.TUESDAY)).thenReturn(true);

        assertThrows(ConflictException.class, () -> routineService.update(
                1L, command(DayOfWeek.TUESDAY, exercise(10L, "Sentadilla", null))));
    }

    @Test
    void givenADayWithoutRoutine_whenGetForDay_thenThrowNotFoundSayingWhichDay() {
        when(routineRepository.findByDayOfWeek(DayOfWeek.WEDNESDAY)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> routineService.getForDay(DayOfWeek.WEDNESDAY));

        assertEquals("No hay rutina asignada para el miércoles", exception.getMessage());
    }

    @Test
    void givenAnExerciseWithAUrl_whenAttachVideoFile_thenTheFileReplacesTheUrl() {
        ExerciseEntity exercise = storedExercise(10L, 0, "Sentadilla", "https://youtu.be/abc", null);
        when(routineRepository.findWithDetailsById(1L)).thenReturn(Optional.of(routine(DayOfWeek.MONDAY, exercise)));
        VideoUpload upload = new VideoUpload(new ByteArrayInputStream(new byte[] {1}), "video/mp4", 1);
        when(videoStorageService.store(upload)).thenReturn("cccc.mp4");

        routineService.attachVideoFile(1L, 10L, upload);

        assertEquals("cccc.mp4", exercise.getVideoFile());
        assertNull(exercise.getVideoUrl());
    }

    @Test
    void givenAnExerciseWithAFile_whenRemoveVideo_thenClearItAndDeleteTheFile() {
        ExerciseEntity exercise = storedExercise(10L, 0, "Sentadilla", null, "aaaa.mp4");
        when(routineRepository.findWithDetailsById(1L)).thenReturn(Optional.of(routine(DayOfWeek.MONDAY, exercise)));

        routineService.removeVideo(1L, 10L);

        assertNull(exercise.getVideoFile());
        verify(videoStorageService).deleteAfterCommit("aaaa.mp4");
    }

    @Test
    void givenAnUnknownExercise_whenRemoveVideo_thenThrowNotFoundException() {
        when(routineRepository.findWithDetailsById(1L))
                .thenReturn(Optional.of(routine(DayOfWeek.MONDAY, storedExercise(10L, 0, "Sentadilla", null, null))));

        assertThrows(NotFoundException.class, () -> routineService.removeVideo(1L, 99L));
    }

    private RoutineCommand command(DayOfWeek dayOfWeek, ExerciseCommand... exercises) {
        return new RoutineCommand(dayOfWeek, "Pierna", "Calentar 10 minutos", 3L, List.of(exercises));
    }

    private ExerciseCommand exercise(Long id, String name, String videoUrl) {
        return new ExerciseCommand(id, name, 4, "10-12", 90, videoUrl);
    }

    private ExerciseEntity storedExercise(Long id, int sortOrder, String name, String videoUrl, String videoFile) {
        return ExerciseEntity.builder()
                .id(id)
                .sortOrder(sortOrder)
                .name(name)
                .sets(4)
                .reps("10-12")
                .restSeconds(90)
                .videoUrl(videoUrl)
                .videoFile(videoFile)
                .build();
    }

    private RoutineEntity routine(DayOfWeek dayOfWeek, ExerciseEntity... exercises) {
        return RoutineEntity.builder()
                .id(1L)
                .dayOfWeek(dayOfWeek)
                .title("Pierna")
                .coach(coach)
                .createdAt(Instant.now())
                .exercises(new ArrayList<>(List.of(exercises)))
                .build();
    }
}
