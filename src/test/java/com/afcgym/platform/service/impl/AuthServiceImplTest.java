package com.afcgym.platform.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.afcgym.platform.configuration.security.SecurityProperties;
import com.afcgym.platform.configuration.security.jwt.TokenClaims;
import com.afcgym.platform.exception.UnauthorizedException;
import com.afcgym.platform.persistance.entities.JwtBlocklistEntity;
import com.afcgym.platform.persistance.entities.UserEntity;
import com.afcgym.platform.persistance.entities.enums.Role;
import com.afcgym.platform.persistance.repositories.IJwtBlocklistRepository;
import com.afcgym.platform.persistance.repositories.IUserRepository;
import com.afcgym.platform.service.dto.LoginResult;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    private static final String SESSION_ENDED_MESSAGE = "Tu sesión terminó. Inicia sesión de nuevo";

    @Mock
    private IUserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtEncoder jwtEncoder;

    @Mock
    private SecurityProperties properties;

    @Mock
    private IJwtBlocklistRepository blocklistRepository;

    @InjectMocks
    private AuthServiceImpl authService;

    @Captor
    private ArgumentCaptor<JwtEncoderParameters> parametersCaptor;

    @Captor
    private ArgumentCaptor<JwtBlocklistEntity> blocklistCaptor;

    private UserEntity activeUser;

    @BeforeEach
    void init() {
        activeUser = user(1L, "admin", true);
    }

    @Test
    void givenValidCredentials_whenLogin_thenStartASessionWhoseTokenExpiresAfterTheIdleTime() {
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(activeUser));
        when(passwordEncoder.matches("Secret123!", "hashed-password")).thenReturn(true);
        when(properties.jwtTtlMinutes()).thenReturn(60L);
        when(properties.sessionMaxHours()).thenReturn(12L);
        when(jwtEncoder.encode(parametersCaptor.capture())).thenReturn(encodedToken());

        LoginResult result = authService.login("admin", "Secret123!");

        assertEquals("token-value", result.token());
        assertEquals(Role.ADMIN, result.role());
        assertEquals(3600, result.expiresInSeconds());
        JwtClaimsSet claims = parametersCaptor.getValue().getClaims();
        assertNotNull(claims.getClaimAsString(TokenClaims.SESSION_ID));
        Long sessionStartedAt = claims.getClaim(TokenClaims.SESSION_STARTED_AT);
        assertEquals(claims.getIssuedAt().getEpochSecond(), sessionStartedAt);
        assertEquals("ADMIN", claims.getClaimAsString(TokenClaims.ROLE));
    }

    @Test
    void givenAnUnknownUsername_whenLogin_thenThrowUnauthorizedException() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThrows(UnauthorizedException.class, () -> authService.login("ghost", "whatever"));

        verify(jwtEncoder, never()).encode(any());
    }

    @Test
    void givenAnInactiveUser_whenLogin_thenThrowUnauthorizedException() {
        when(userRepository.findByUsername("inactive")).thenReturn(Optional.of(user(2L, "inactive", false)));

        assertThrows(UnauthorizedException.class, () -> authService.login("inactive", "Secret123!"));

        verify(jwtEncoder, never()).encode(any());
    }

    @Test
    void givenAWrongPassword_whenLogin_thenThrowUnauthorizedException() {
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(activeUser));
        when(passwordEncoder.matches("wrong-password", "hashed-password")).thenReturn(false);

        assertThrows(UnauthorizedException.class, () -> authService.login("admin", "wrong-password"));

        verify(jwtEncoder, never()).encode(any());
    }

    @Test
    void givenAnActiveSession_whenRefresh_thenKeepTheSessionAndRestartTheIdleTime() {
        Instant sessionStartedAt = Instant.now().minus(2, ChronoUnit.HOURS).truncatedTo(ChronoUnit.SECONDS);
        when(properties.jwtTtlMinutes()).thenReturn(60L);
        when(properties.sessionMaxHours()).thenReturn(12L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(activeUser));
        when(jwtEncoder.encode(parametersCaptor.capture())).thenReturn(encodedToken());

        LoginResult result = authService.refresh(sessionToken("session-1", sessionStartedAt, 1L));

        assertEquals("token-value", result.token());
        assertEquals(3600, result.expiresInSeconds());
        JwtClaimsSet claims = parametersCaptor.getValue().getClaims();
        assertEquals("session-1", claims.getClaimAsString(TokenClaims.SESSION_ID));
        Long refreshedSessionStart = claims.getClaim(TokenClaims.SESSION_STARTED_AT);
        assertEquals(sessionStartedAt.getEpochSecond(), refreshedSessionStart);
        assertNotEquals("old-token-id", claims.getId());
    }

    @Test
    void givenASessionCloseToItsMaximum_whenRefresh_thenTheTokenExpiresWhenTheSessionEnds() {
        Instant sessionStartedAt = Instant.now().minus(12, ChronoUnit.HOURS).plus(10, ChronoUnit.MINUTES);
        when(properties.jwtTtlMinutes()).thenReturn(60L);
        when(properties.sessionMaxHours()).thenReturn(12L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(activeUser));
        when(jwtEncoder.encode(any(JwtEncoderParameters.class))).thenReturn(encodedToken());

        LoginResult result = authService.refresh(sessionToken("session-1", sessionStartedAt, 1L));

        assertTrue(result.expiresInSeconds() > 580 && result.expiresInSeconds() <= 600);
    }

    @Test
    void givenASessionPastItsMaximum_whenRefresh_thenThrowUnauthorizedException() {
        Instant sessionStartedAt = Instant.now().minus(13, ChronoUnit.HOURS);
        when(properties.sessionMaxHours()).thenReturn(12L);

        UnauthorizedException exception = assertThrows(UnauthorizedException.class,
                () -> authService.refresh(sessionToken("session-1", sessionStartedAt, 1L)));

        assertEquals(SESSION_ENDED_MESSAGE, exception.getMessage());
        verify(jwtEncoder, never()).encode(any());
    }

    @Test
    void givenADeactivatedUser_whenRefresh_thenThrowUnauthorizedException() {
        when(properties.sessionMaxHours()).thenReturn(12L);
        when(userRepository.findById(2L)).thenReturn(Optional.of(user(2L, "inactive", false)));

        assertThrows(UnauthorizedException.class,
                () -> authService.refresh(sessionToken("session-1", Instant.now(), 2L)));

        verify(jwtEncoder, never()).encode(any());
    }

    @Test
    void givenAPasswordChangedAfterTheSessionStarted_whenRefresh_thenThrowUnauthorizedException() {
        Instant sessionStartedAt = Instant.now().minus(2, ChronoUnit.HOURS).truncatedTo(ChronoUnit.SECONDS);
        activeUser.changePassword("new-hash", Instant.now().minus(1, ChronoUnit.HOURS));
        when(properties.sessionMaxHours()).thenReturn(12L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(activeUser));

        assertThrows(UnauthorizedException.class,
                () -> authService.refresh(sessionToken("session-1", sessionStartedAt, 1L)));

        verify(jwtEncoder, never()).encode(any());
    }

    @Test
    void givenAPasswordChangedBeforeTheSessionStarted_whenRefresh_thenIssueANewToken() {
        Instant sessionStartedAt = Instant.now().minus(1, ChronoUnit.HOURS).truncatedTo(ChronoUnit.SECONDS);
        activeUser.changePassword("new-hash", Instant.now().minus(2, ChronoUnit.HOURS));
        when(properties.jwtTtlMinutes()).thenReturn(60L);
        when(properties.sessionMaxHours()).thenReturn(12L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(activeUser));
        when(jwtEncoder.encode(any(JwtEncoderParameters.class))).thenReturn(encodedToken());

        assertEquals("token-value", authService.refresh(sessionToken("session-1", sessionStartedAt, 1L)).token());
    }

    @Test
    void givenATokenWithoutSession_whenRefresh_thenThrowUnauthorizedException() {
        Jwt legacyToken = Jwt.withTokenValue("legacy-token")
                .header("alg", "HS256")
                .claim("jti", "legacy-id")
                .claim(TokenClaims.USER_ID, 1L)
                .build();

        assertThrows(UnauthorizedException.class, () -> authService.refresh(legacyToken));

        verify(jwtEncoder, never()).encode(any());
    }

    @Test
    void givenASessionToken_whenLogout_thenBlockTheWholeSessionUntilItsMaximumEnd() {
        Instant sessionStartedAt = Instant.now().minus(1, ChronoUnit.HOURS).truncatedTo(ChronoUnit.SECONDS);
        when(properties.sessionMaxHours()).thenReturn(12L);

        authService.logout(sessionToken("session-1", sessionStartedAt, 1L));

        verify(blocklistRepository).save(blocklistCaptor.capture());
        assertEquals("session-1", blocklistCaptor.getValue().getJti());
        assertEquals(sessionStartedAt.plus(12, ChronoUnit.HOURS), blocklistCaptor.getValue().getExpiresAt());
    }

    @Test
    void givenATokenWithoutSession_whenLogout_thenBlockOnlyThatToken() {
        Instant expiresAt = Instant.now().plusSeconds(3600);
        Jwt token = Jwt.withTokenValue("token-value")
                .header("alg", "HS256")
                .claim("sub", "admin")
                .claim("jti", "jti-123")
                .expiresAt(expiresAt)
                .issuedAt(Instant.now())
                .build();

        authService.logout(token);

        verify(blocklistRepository).save(blocklistCaptor.capture());
        assertEquals("jti-123", blocklistCaptor.getValue().getJti());
        assertEquals(expiresAt, blocklistCaptor.getValue().getExpiresAt());
    }

    private Jwt sessionToken(String sessionId, Instant sessionStartedAt, Long userId) {
        return Jwt.withTokenValue("old-token")
                .header("alg", "HS256")
                .claim("jti", "old-token-id")
                .claim(TokenClaims.USER_ID, userId)
                .claim(TokenClaims.ROLE, Role.ADMIN.name())
                .claim(TokenClaims.SESSION_ID, sessionId)
                .claim(TokenClaims.SESSION_STARTED_AT, sessionStartedAt.getEpochSecond())
                .issuedAt(Instant.now().minus(5, ChronoUnit.MINUTES))
                .expiresAt(Instant.now().plus(55, ChronoUnit.MINUTES))
                .build();
    }

    private Jwt encodedToken() {
        return Jwt.withTokenValue("token-value")
                .header("alg", "HS256")
                .claim("sub", "admin")
                .build();
    }

    private UserEntity user(Long id, String username, boolean active) {
        return UserEntity.builder()
                .id(id)
                .username(username)
                .fullName("Usuario " + username)
                .email(username + "@afcgym.com")
                .passwordHash("hashed-password")
                .role(Role.ADMIN)
                .active(active)
                .createdAt(Instant.now())
                .build();
    }
}
