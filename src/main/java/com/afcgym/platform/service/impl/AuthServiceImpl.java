package com.afcgym.platform.service.impl;

import com.afcgym.platform.configuration.security.SecurityProperties;
import com.afcgym.platform.configuration.security.jwt.TokenClaims;
import com.afcgym.platform.exception.UnauthorizedException;
import com.afcgym.platform.persistance.entities.JwtBlocklistEntity;
import com.afcgym.platform.persistance.entities.UserEntity;
import com.afcgym.platform.persistance.repositories.IJwtBlocklistRepository;
import com.afcgym.platform.persistance.repositories.IUserRepository;
import com.afcgym.platform.service.IAuthService;
import com.afcgym.platform.service.dto.LoginResult;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements IAuthService {

    private static final String ISSUER = "afc-gym-platform";
    private static final String GENERIC_LOGIN_ERROR = "Usuario o contraseña incorrectos";
    private static final String SESSION_ENDED_ERROR = "Tu sesión terminó. Inicia sesión de nuevo";

    private final IUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtEncoder jwtEncoder;
    private final SecurityProperties properties;
    private final IJwtBlocklistRepository blocklistRepository;

    @Override
    public LoginResult login(String username, String password) {
        Optional<UserEntity> user = userRepository.findByUsername(username);
        if (user.isEmpty() || !user.get().isActive() || !passwordEncoder.matches(password, user.get().getPasswordHash())) {
            throw new UnauthorizedException(GENERIC_LOGIN_ERROR);
        }
        Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        return issueToken(user.get(), UUID.randomUUID().toString(), now, now);
    }

    @Override
    @Transactional(readOnly = true)
    public LoginResult refresh(Jwt token) {
        String sessionId = token.getClaimAsString(TokenClaims.SESSION_ID);
        Instant sessionStartedAt = token.getClaimAsInstant(TokenClaims.SESSION_STARTED_AT);
        Long userId = token.getClaim(TokenClaims.USER_ID);
        Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        if (sessionId == null || sessionStartedAt == null || userId == null
                || !now.isBefore(sessionEnd(sessionStartedAt))) {
            throw new UnauthorizedException(SESSION_ENDED_ERROR);
        }
        UserEntity user = userRepository.findById(userId)
                .filter(UserEntity::isActive)
                .orElseThrow(() -> new UnauthorizedException(SESSION_ENDED_ERROR));
        return issueToken(user, sessionId, sessionStartedAt, now);
    }

    @Override
    @Transactional
    public void logout(Jwt token) {
        String sessionId = token.getClaimAsString(TokenClaims.SESSION_ID);
        Instant sessionStartedAt = token.getClaimAsInstant(TokenClaims.SESSION_STARTED_AT);
        if (sessionId == null || sessionStartedAt == null) {
            revoke(token.getId(), token.getExpiresAt());
            return;
        }
        Instant sessionEnd = sessionEnd(sessionStartedAt);
        Instant tokenExpiresAt = token.getExpiresAt();
        revoke(sessionId, tokenExpiresAt != null && tokenExpiresAt.isAfter(sessionEnd) ? tokenExpiresAt : sessionEnd);
    }

    private LoginResult issueToken(UserEntity user, String sessionId, Instant sessionStartedAt, Instant now) {
        Instant idleLimit = now.plus(properties.jwtTtlMinutes(), ChronoUnit.MINUTES);
        Instant sessionEnd = sessionEnd(sessionStartedAt);
        Instant expiresAt = idleLimit.isBefore(sessionEnd) ? idleLimit : sessionEnd;
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .id(UUID.randomUUID().toString())
                .issuer(ISSUER)
                .subject(user.getUsername())
                .issuedAt(now)
                .expiresAt(expiresAt)
                .claim(TokenClaims.USER_ID, user.getId())
                .claim(TokenClaims.ROLE, user.getRole().name())
                .claim(TokenClaims.SESSION_ID, sessionId)
                .claim(TokenClaims.SESSION_STARTED_AT, sessionStartedAt.getEpochSecond())
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new LoginResult(token, user.getRole(), ChronoUnit.SECONDS.between(now, expiresAt));
    }

    private Instant sessionEnd(Instant sessionStartedAt) {
        return sessionStartedAt.plus(properties.sessionMaxHours(), ChronoUnit.HOURS);
    }

    private void revoke(String identifier, Instant expiresAt) {
        blocklistRepository.save(JwtBlocklistEntity.builder()
                .jti(identifier)
                .expiresAt(expiresAt)
                .build());
    }
}
