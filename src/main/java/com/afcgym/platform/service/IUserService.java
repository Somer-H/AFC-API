package com.afcgym.platform.service;

import com.afcgym.platform.persistance.entities.UserEntity;
import com.afcgym.platform.service.dto.CreateUserCommand;

public interface IUserService {

    UserEntity createAdmin(CreateUserCommand command);

    UserEntity getById(Long id);
}
