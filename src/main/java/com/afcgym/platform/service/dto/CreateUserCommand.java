package com.afcgym.platform.service.dto;

import com.afcgym.platform.persistance.entities.enums.Role;

public record CreateUserCommand(
        String username,
        String fullName,
        String email,
        String password,
        Role role
) {}
