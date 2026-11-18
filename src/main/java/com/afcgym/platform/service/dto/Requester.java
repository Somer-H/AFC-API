package com.afcgym.platform.service.dto;

import com.afcgym.platform.persistance.entities.enums.Role;

public record Requester(Long userId, Role role) {

    public boolean isAdmin() {
        return role == Role.ADMIN;
    }
}
