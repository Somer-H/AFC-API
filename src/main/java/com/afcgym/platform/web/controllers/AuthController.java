package com.afcgym.platform.web.controllers;

import com.afcgym.platform.service.IAuthService;
import com.afcgym.platform.service.IPasswordResetService;
import com.afcgym.platform.service.dto.LoginResult;
import com.afcgym.platform.web.dto.request.ConfirmPasswordResetRequest;
import com.afcgym.platform.web.dto.request.LoginRequest;
import com.afcgym.platform.web.dto.request.RequestPasswordResetRequest;
import com.afcgym.platform.web.dto.response.LoginResponse;
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
@RequestMapping("/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Auth")
public class AuthController {

    private final IAuthService authService;
    private final IPasswordResetService passwordResetService;

    @PostMapping("/login")
    @Operation(summary = "Log in with username and password")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        LoginResult result = authService.login(request.username(), request.password());
        return LoginResponse.builder()
                .token(result.token())
                .role(result.role().name())
                .expiresInSeconds(result.expiresInSeconds())
                .build();
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Log out and invalidate the active token")
    public void logout(@AuthenticationPrincipal Jwt jwt) {
        authService.logout(jwt);
    }

    @PostMapping("/password-resets")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Request a password reset")
    public void requestPasswordReset(@Valid @RequestBody RequestPasswordResetRequest request) {
        passwordResetService.requestReset(request.email());
    }

    @PostMapping("/password-resets/confirm")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Confirm a password reset with its token")
    public void confirmPasswordReset(@Valid @RequestBody ConfirmPasswordResetRequest request) {
        passwordResetService.confirmReset(request.token(), request.newPassword());
    }
}
