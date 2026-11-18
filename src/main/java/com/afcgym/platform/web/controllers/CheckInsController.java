package com.afcgym.platform.web.controllers;

import com.afcgym.platform.mapper.CheckInMapper;
import com.afcgym.platform.service.ICheckInService;
import com.afcgym.platform.web.dto.request.CheckInRequest;
import com.afcgym.platform.web.dto.response.CheckInResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/check-ins")
@RequiredArgsConstructor
@Tag(name = "Check-ins")
public class CheckInsController {

    private final ICheckInService checkInService;
    private final CheckInMapper checkInMapper;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Check in a member by PIN or fingerprint token")
    public CheckInResponse checkIn(@Valid @RequestBody CheckInRequest request) {
        return checkInMapper.toResponse(checkInService.checkIn(
                request.memberId(),
                request.pin(),
                request.fingerprintToken()));
    }
}
