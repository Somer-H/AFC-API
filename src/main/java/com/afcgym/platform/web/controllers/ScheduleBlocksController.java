package com.afcgym.platform.web.controllers;

import com.afcgym.platform.mapper.ScheduleBlockMapper;
import com.afcgym.platform.service.IScheduleBlockService;
import com.afcgym.platform.service.dto.ScheduleBlockCommand;
import com.afcgym.platform.web.dto.request.ScheduleBlockRequest;
import com.afcgym.platform.web.dto.response.DayOccupancyResponse;
import com.afcgym.platform.web.dto.response.ScheduleBlockResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/schedule-blocks")
@RequiredArgsConstructor
@Tag(name = "Schedule blocks")
public class ScheduleBlocksController {

    private final IScheduleBlockService scheduleBlockService;
    private final ScheduleBlockMapper scheduleBlockMapper;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a weekly schedule block with its capacity and optional coach")
    public ScheduleBlockResponse create(@Valid @RequestBody ScheduleBlockRequest request) {
        return scheduleBlockMapper.toResponse(scheduleBlockService.create(toCommand(request)));
    }

    @GetMapping
    @Operation(summary = "List schedule blocks, optionally for one day of the week")
    public List<ScheduleBlockResponse> list(@RequestParam(required = false) DayOfWeek dayOfWeek) {
        return scheduleBlockService.list(dayOfWeek).stream().map(scheduleBlockMapper::toResponse).toList();
    }

    @GetMapping("/occupancy")
    @Operation(summary = "Occupancy of every block on a date (or the next given day of the week, or today)")
    public DayOccupancyResponse occupancy(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) DayOfWeek dayOfWeek) {
        return scheduleBlockMapper.toDayOccupancyResponse(scheduleBlockService.occupancy(date, dayOfWeek));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a schedule block")
    public ScheduleBlockResponse getById(@PathVariable Long id) {
        return scheduleBlockMapper.toResponse(scheduleBlockService.getById(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a schedule block")
    public ScheduleBlockResponse update(@PathVariable Long id, @Valid @RequestBody ScheduleBlockRequest request) {
        return scheduleBlockMapper.toResponse(scheduleBlockService.update(id, toCommand(request)));
    }

    @PatchMapping("/{id}/activate")
    @Operation(summary = "Activate a schedule block so it accepts bookings again")
    public ScheduleBlockResponse activate(@PathVariable Long id) {
        return scheduleBlockMapper.toResponse(scheduleBlockService.activate(id));
    }

    @PatchMapping("/{id}/deactivate")
    @Operation(summary = "Deactivate a schedule block without touching its existing bookings")
    public ScheduleBlockResponse deactivate(@PathVariable Long id) {
        return scheduleBlockMapper.toResponse(scheduleBlockService.deactivate(id));
    }

    private ScheduleBlockCommand toCommand(ScheduleBlockRequest request) {
        return new ScheduleBlockCommand(
                request.dayOfWeek(),
                request.startTime(),
                request.endTime(),
                request.maxCapacity(),
                request.coachId());
    }
}
