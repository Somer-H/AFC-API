package com.afcgym.platform.web.controllers;

import com.afcgym.platform.configuration.security.AuthenticatedUser;
import com.afcgym.platform.mapper.UserMapper;
import com.afcgym.platform.persistance.entities.enums.Role;
import com.afcgym.platform.service.IUserService;
import com.afcgym.platform.service.dto.CreateUserCommand;
import com.afcgym.platform.web.dto.request.CreateUserRequest;
import com.afcgym.platform.web.dto.response.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/users")
@RequiredArgsConstructor
@Tag(name = "Users")
public class UsersController {

    private final IUserService userService;
    private final UserMapper userMapper;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create another admin account")
    public UserResponse create(@Valid @RequestBody CreateUserRequest request) {
        return userMapper.toResponse(userService.createAdmin(new CreateUserCommand(
                request.username(),
                request.fullName(),
                request.email(),
                request.password(),
                Role.ADMIN)));
    }

    @GetMapping("/me")
    @Operation(summary = "Get the authenticated user's own profile")
    public UserResponse me(@AuthenticationPrincipal Jwt jwt) {
        AuthenticatedUser authenticatedUser = AuthenticatedUser.fromJwt(jwt);
        return userMapper.toResponse(userService.getById(authenticatedUser.userId()));
    }
}
