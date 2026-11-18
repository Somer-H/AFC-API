package com.afcgym.platform.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.afcgym.platform.exception.ConflictException;
import com.afcgym.platform.exception.NotFoundException;
import com.afcgym.platform.persistance.entities.UserEntity;
import com.afcgym.platform.persistance.entities.enums.Role;
import com.afcgym.platform.persistance.repositories.IUserRepository;
import com.afcgym.platform.service.dto.CreateUserCommand;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private IUserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserServiceImpl userService;

    private CreateUserCommand command;
    private UserEntity existingUser;

    @BeforeEach
    void init() {
        command = new CreateUserCommand("newadmin", "New Admin", "new@afcgym.com", "Secret123!", Role.ADMIN);
        existingUser = createUserDummy();
    }

    @Test
    void givenANewUsername_whenCreateAdmin_thenSaveAndReturnTheUser() {
        when(userRepository.existsByUsername(command.username())).thenReturn(false);
        when(userRepository.existsByEmail(command.email())).thenReturn(false);
        when(passwordEncoder.encode(command.password())).thenReturn("hashed-password");
        when(userRepository.save(any(UserEntity.class))).thenReturn(existingUser);

        UserEntity result = userService.createAdmin(command);

        verify(userRepository, never()).findByUsername(any());
        assertEquals(existingUser, result);
    }

    @Test
    void givenAnExistingUsername_whenCreateAdmin_thenThrowConflictException() {
        when(userRepository.existsByUsername(command.username())).thenReturn(true);

        assertThrows(ConflictException.class, () -> userService.createAdmin(command));

        verify(userRepository, never()).save(any());
    }

    @Test
    void givenAnExistingEmail_whenCreateAdmin_thenThrowConflictException() {
        when(userRepository.existsByUsername(command.username())).thenReturn(false);
        when(userRepository.existsByEmail(command.email())).thenReturn(true);

        assertThrows(ConflictException.class, () -> userService.createAdmin(command));

        verify(userRepository, never()).save(any());
    }

    @Test
    void givenAnExistingId_whenGetById_thenReturnTheUser() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(existingUser));

        UserEntity result = userService.getById(1L);

        assertEquals(existingUser, result);
    }

    @Test
    void givenAnUnknownId_whenGetById_thenThrowNotFoundException() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> userService.getById(99L));
    }

    private UserEntity createUserDummy() {
        return UserEntity.builder()
                .id(1L)
                .username("admin")
                .fullName("Administrador")
                .email("admin@afcgym.com")
                .passwordHash("hashed-password")
                .role(Role.ADMIN)
                .active(true)
                .createdAt(Instant.now())
                .build();
    }
}
