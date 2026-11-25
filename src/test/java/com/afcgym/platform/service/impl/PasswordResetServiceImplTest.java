package com.afcgym.platform.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.afcgym.platform.exception.BadRequestException;
import com.afcgym.platform.persistance.entities.PasswordResetTokenEntity;
import com.afcgym.platform.persistance.entities.UserEntity;
import com.afcgym.platform.persistance.entities.enums.Role;
import com.afcgym.platform.persistance.repositories.IPasswordResetTokenRepository;
import com.afcgym.platform.persistance.repositories.IUserRepository;
import com.afcgym.platform.service.dto.PasswordResetRequestedEvent;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceImplTest {

    @Mock
    private IUserRepository userRepository;

    @Mock
    private IPasswordResetTokenRepository passwordResetTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private PasswordResetServiceImpl passwordResetService;

    private UserEntity activeUser;

    @BeforeEach
    void init() {
        activeUser = UserEntity.builder()
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

    @Test
    void givenAKnownActiveEmail_whenRequestReset_thenSaveAToken() {
        when(userRepository.findByEmail("admin@afcgym.com")).thenReturn(Optional.of(activeUser));

        passwordResetService.requestReset("admin@afcgym.com");

        ArgumentCaptor<PasswordResetTokenEntity> savedToken = ArgumentCaptor.forClass(PasswordResetTokenEntity.class);
        verify(passwordResetTokenRepository).save(savedToken.capture());
        ArgumentCaptor<PasswordResetRequestedEvent> event = ArgumentCaptor.forClass(PasswordResetRequestedEvent.class);
        verify(eventPublisher).publishEvent(event.capture());
        assertEquals("admin@afcgym.com", event.getValue().email());
        assertEquals("Administrador", event.getValue().fullName());
        assertEquals(savedToken.getValue().getToken(), event.getValue().token());
        assertEquals(savedToken.getValue().getExpiresAt(), event.getValue().expiresAt());
    }

    @Test
    void givenAnUnknownEmail_whenRequestReset_thenDoNothing() {
        when(userRepository.findByEmail("ghost@afcgym.com")).thenReturn(Optional.empty());

        passwordResetService.requestReset("ghost@afcgym.com");

        verify(passwordResetTokenRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void givenAValidToken_whenConfirmReset_thenUpdateThePassword() {
        PasswordResetTokenEntity token = createTokenDummy(Instant.now().plus(10, ChronoUnit.MINUTES), null);
        when(passwordResetTokenRepository.findByToken("valid-token")).thenReturn(Optional.of(token));
        when(userRepository.findById(1L)).thenReturn(Optional.of(activeUser));
        when(passwordEncoder.encode("NewSecret123!")).thenReturn("new-hashed-password");

        passwordResetService.confirmReset("valid-token", "NewSecret123!");

        assertEquals("new-hashed-password", activeUser.getPasswordHash());
        assertEquals(true, token.isUsed());
    }

    @Test
    void givenAnUnknownToken_whenConfirmReset_thenThrowBadRequestException() {
        when(passwordResetTokenRepository.findByToken("unknown")).thenReturn(Optional.empty());

        assertThrows(BadRequestException.class, () -> passwordResetService.confirmReset("unknown", "NewSecret123!"));
    }

    @Test
    void givenAnExpiredToken_whenConfirmReset_thenThrowBadRequestException() {
        PasswordResetTokenEntity token = createTokenDummy(Instant.now().minus(1, ChronoUnit.MINUTES), null);
        when(passwordResetTokenRepository.findByToken("expired-token")).thenReturn(Optional.of(token));

        assertThrows(BadRequestException.class,
                () -> passwordResetService.confirmReset("expired-token", "NewSecret123!"));
    }

    @Test
    void givenAnAlreadyUsedToken_whenConfirmReset_thenThrowBadRequestException() {
        PasswordResetTokenEntity token =
                createTokenDummy(Instant.now().plus(10, ChronoUnit.MINUTES), Instant.now());
        when(passwordResetTokenRepository.findByToken("used-token")).thenReturn(Optional.of(token));

        assertThrows(BadRequestException.class,
                () -> passwordResetService.confirmReset("used-token", "NewSecret123!"));
    }

    private PasswordResetTokenEntity createTokenDummy(Instant expiresAt, Instant usedAt) {
        return PasswordResetTokenEntity.builder()
                .id(1L)
                .userId(1L)
                .token("some-token")
                .expiresAt(expiresAt)
                .usedAt(usedAt)
                .createdAt(Instant.now())
                .build();
    }
}
