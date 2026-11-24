package com.afcgym.platform.web.controllers;

import com.afcgym.platform.mapper.RoutineMapper;
import com.afcgym.platform.service.IRoutineService;
import com.afcgym.platform.service.dto.ExerciseCommand;
import com.afcgym.platform.service.dto.RoutineCommand;
import com.afcgym.platform.service.dto.VideoUpload;
import com.afcgym.platform.web.dto.request.RoutineRequest;
import com.afcgym.platform.web.dto.response.RoutineResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.io.IOException;
import java.io.InputStream;
import java.time.DayOfWeek;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/v1/routines")
@RequiredArgsConstructor
@Tag(name = "Routines")
public class RoutinesController {

    private final IRoutineService routineService;
    private final RoutineMapper routineMapper;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create the routine of a day of the week (one per day, 1 to 8 exercises)")
    public RoutineResponse create(@Valid @RequestBody RoutineRequest request) {
        return routineMapper.toResponse(routineService.create(toCommand(request)));
    }

    @GetMapping
    @Operation(summary = "List every routine, Monday first")
    public List<RoutineResponse> list() {
        return routineService.list().stream().map(routineMapper::toResponse).toList();
    }

    @GetMapping("/by-day")
    @Operation(summary = "Routine for a day of the week (today when no day is given)")
    public RoutineResponse getForDay(@RequestParam(required = false) DayOfWeek dayOfWeek) {
        return routineMapper.toResponse(routineService.getForDay(dayOfWeek));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a routine with its exercises")
    public RoutineResponse getById(@PathVariable Long id) {
        return routineMapper.toResponse(routineService.getById(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Replace a routine; exercises sent with their id are kept, the rest are added or removed")
    public RoutineResponse update(@PathVariable Long id, @Valid @RequestBody RoutineRequest request) {
        return routineMapper.toResponse(routineService.update(id, toCommand(request)));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a routine and free its day")
    public void delete(@PathVariable Long id) {
        routineService.delete(id);
    }

    @PostMapping(path = "/{id}/exercises/{exerciseId}/video", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload an MP4, MOV or WEBM video for an exercise; it replaces any video URL")
    public RoutineResponse uploadVideo(
            @PathVariable Long id,
            @PathVariable Long exerciseId,
            @RequestPart("file") MultipartFile file) throws IOException {
        try (InputStream content = file.getInputStream()) {
            return routineMapper.toResponse(routineService.attachVideoFile(
                    id, exerciseId, new VideoUpload(content, file.getContentType(), file.getSize())));
        }
    }

    @DeleteMapping("/{id}/exercises/{exerciseId}/video")
    @Operation(summary = "Remove the video of an exercise, whether it is a URL or an uploaded file")
    public RoutineResponse removeVideo(@PathVariable Long id, @PathVariable Long exerciseId) {
        return routineMapper.toResponse(routineService.removeVideo(id, exerciseId));
    }

    private RoutineCommand toCommand(RoutineRequest request) {
        return new RoutineCommand(
                request.dayOfWeek(),
                request.title(),
                request.description(),
                request.coachId(),
                request.exercises().stream()
                        .map(exercise -> new ExerciseCommand(
                                exercise.id(),
                                exercise.name(),
                                exercise.sets(),
                                exercise.reps(),
                                exercise.restSeconds(),
                                exercise.videoUrl()))
                        .toList());
    }
}
