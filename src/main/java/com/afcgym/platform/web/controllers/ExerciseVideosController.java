package com.afcgym.platform.web.controllers;

import com.afcgym.platform.service.IVideoStorageService;
import com.afcgym.platform.service.dto.StoredVideo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/exercise-videos")
@RequiredArgsConstructor
@Tag(name = "Exercise videos")
public class ExerciseVideosController {

    private final IVideoStorageService videoStorageService;

    @GetMapping("/{fileName}")
    @Operation(summary = "Stream an uploaded exercise video (supports HTTP Range for players)")
    public ResponseEntity<Resource> get(@PathVariable String fileName) {
        StoredVideo video = videoStorageService.load(fileName);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(video.contentType()))
                .body(video.resource());
    }
}
