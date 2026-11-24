package com.afcgym.platform.web.controllers;

import com.afcgym.platform.mapper.CoachMapper;
import com.afcgym.platform.service.ICoachService;
import com.afcgym.platform.service.dto.CreateCoachCommand;
import com.afcgym.platform.web.dto.request.CreateCoachRequest;
import com.afcgym.platform.web.dto.response.CoachResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/coaches")
@RequiredArgsConstructor
@Tag(name = "Coaches")
public class CoachesController {

    private final ICoachService coachService;
    private final CoachMapper coachMapper;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a coach")
    public CoachResponse create(@Valid @RequestBody CreateCoachRequest request) {
        return coachMapper.toResponse(coachService.create(new CreateCoachCommand(request.name(), request.avatarUrl())));
    }

    @GetMapping
    @Operation(summary = "List coaches by name")
    public List<CoachResponse> list() {
        return coachService.list().stream().map(coachMapper::toResponse).toList();
    }
}
