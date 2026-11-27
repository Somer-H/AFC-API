package com.afcgym.platform.service.impl;

import com.afcgym.platform.exception.BadRequestException;
import com.afcgym.platform.persistance.entities.PasswordResetTokenEntity;
import com.afcgym.platform.persistance.entities.UserEntity;
import com.afcgym.platform.persistance.repositories.IPasswordResetTokenRepository;
import com.afcgym.platform.persistance.repositories.IUserRepository;
import com.afcgym.platform.service.IPasswordResetService;
import com.afcgym.platform.service.dto.PasswordResetRequestedEvent;
import com.afcgym.platform.utils.SecureTokenGenerator;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PasswordResetServiceImpl implements IPasswordResetService {

    private static final long TOKEN_TTL_MINUTES = 30;
    private static final int TOKEN_BYTES = 32;
    private static final String INVALID_TOKEN_MESSAGE = "El enlace de recuperación no es válido o ya expiró";

    private final IUserRepository userRepository;
    private final IPasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public void requestReset(String email) {
        Optional<UserEntity> user = userRepository.findByEmail(email);
        if (user.isEmpty() || !user.get().isActive()) {
            return;
        }
        Instant now = Instant.now();
        PasswordResetTokenEntity resetToken = PasswordResetTokenEntity.builder()
                .userId(user.get().getId())
                .token(SecureTokenGenerator.generate(TOKEN_BYTES))
                .expiresAt(now.plus(TOKEN_TTL_MINUTES, ChronoUnit.MINUTES))
                .createdAt(now)
                .build();
        passwordResetTokenRepository.save(resetToken);
        eventPublisher.publishEvent(new PasswordResetRequestedEvent(
                user.get().getEmail(), user.get().getFullName(), resetToken.getToken(), resetToken.getExpiresAt()));
    }

    @Override
    @Transactional
    public void confirmReset(String token, String newPassword) {
        PasswordResetTokenEntity resetToken = passwordResetTokenRepository.findByToken(token)
                .orElseThrow(() -> new BadRequestException(INVALID_TOKEN_MESSAGE));
        if (resetToken.isUsed() || resetToken.getExpiresAt().isBefore(Instant.now())) {
            throw new BadRequestException(INVALID_TOKEN_MESSAGE);
        }
        UserEntity user = userRepository.findById(resetToken.getUserId())
                .orElseThrow(() -> new BadRequestException(INVALID_TOKEN_MESSAGE));
        Instant now = Instant.now();
        user.changePassword(passwordEncoder.encode(newPassword), now);
        resetToken.markAsUsed(now);
    }
}
