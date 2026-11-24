package com.afcgym.platform.web.controllers;

import com.afcgym.platform.configuration.security.AuthenticatedUser;
import com.afcgym.platform.mapper.PushTokenMapper;
import com.afcgym.platform.service.INotificationService;
import com.afcgym.platform.service.dto.Requester;
import com.afcgym.platform.web.dto.request.RegisterPushTokenRequest;
import com.afcgym.platform.web.dto.response.PushTokenResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/push-tokens")
@RequiredArgsConstructor
@Tag(name = "Push notifications")
public class PushTokensController {

    private final INotificationService notificationService;
    private final PushTokenMapper pushTokenMapper;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Register the Expo push token of a device of the signed-in member")
    public PushTokenResponse register(
            @Valid @RequestBody RegisterPushTokenRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        AuthenticatedUser authenticatedUser = AuthenticatedUser.fromJwt(jwt);
        Requester requester = new Requester(authenticatedUser.userId(), authenticatedUser.role());
        return pushTokenMapper.toResponse(notificationService.registerPushToken(request.token(), requester));
    }
}
