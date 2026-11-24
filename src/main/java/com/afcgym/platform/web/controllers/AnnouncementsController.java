package com.afcgym.platform.web.controllers;

import com.afcgym.platform.configuration.security.AuthenticatedUser;
import com.afcgym.platform.mapper.AnnouncementMapper;
import com.afcgym.platform.service.IAnnouncementService;
import com.afcgym.platform.service.dto.PublishAnnouncementCommand;
import com.afcgym.platform.web.dto.request.PublishAnnouncementRequest;
import com.afcgym.platform.web.dto.response.AnnouncementResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/announcements")
@RequiredArgsConstructor
@Tag(name = "Announcements")
public class AnnouncementsController {

    private final IAnnouncementService announcementService;
    private final AnnouncementMapper announcementMapper;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Publish an announcement")
    public AnnouncementResponse publish(
            @Valid @RequestBody PublishAnnouncementRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return announcementMapper.toResponse(announcementService.publish(new PublishAnnouncementCommand(
                request.title(),
                request.message(),
                request.type(),
                request.startDate(),
                request.endDate(),
                AuthenticatedUser.fromJwt(jwt).userId())));
    }

    @GetMapping
    @Operation(summary = "List every announcement, including past and scheduled ones")
    public List<AnnouncementResponse> list() {
        return announcementService.list().stream().map(announcementMapper::toResponse).toList();
    }

    @GetMapping("/active")
    @Operation(summary = "Current announcements not dismissed in this session, most important first")
    public List<AnnouncementResponse> listCurrent(@AuthenticationPrincipal Jwt jwt) {
        return announcementService.listCurrent(jwt.getId()).stream().map(announcementMapper::toResponse).toList();
    }

    @PostMapping("/{id}/dismiss")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Close an announcement so it does not appear again in this session")
    public void dismiss(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        announcementService.dismiss(id, jwt.getId());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete an announcement")
    public void delete(@PathVariable Long id) {
        announcementService.delete(id);
    }
}
