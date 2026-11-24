package com.afcgym.platform.service.impl;

import com.afcgym.platform.exception.ConflictException;
import com.afcgym.platform.exception.NotFoundException;
import com.afcgym.platform.persistance.entities.UserEntity;
import com.afcgym.platform.persistance.repositories.IUserRepository;
import com.afcgym.platform.service.IUserService;
import com.afcgym.platform.service.dto.CreateUserCommand;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements IUserService {

    private final IUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public UserEntity createAdmin(CreateUserCommand command) {
        if (userRepository.existsByUsername(command.username())) {
            throw new ConflictException("Ya existe una cuenta con ese usuario");
        }
        if (userRepository.existsByEmail(command.email())) {
            throw new ConflictException("Ya existe una cuenta con ese correo");
        }
        UserEntity user = UserEntity.builder()
                .username(command.username())
                .fullName(command.fullName())
                .email(command.email())
                .passwordHash(passwordEncoder.encode(command.password()))
                .role(command.role())
                .active(true)
                .createdAt(Instant.now())
                .build();
        return userRepository.save(user);
    }

    @Override
    public UserEntity getById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("No se encontró el usuario solicitado"));
    }
}
