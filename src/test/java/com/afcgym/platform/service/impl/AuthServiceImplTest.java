package com.afcgym.platform.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.afcgym.platform.configuration.security.SecurityProperties;
import com.afcgym.platform.exception.UnauthorizedException;
import com.afcgym.platform.persistance.entities.UserEntity;
import com.afcgym.platform.persistance.entities.enums.Role;
import com.afcgym.platform.persistance.repositories.IJwtBlocklistRepository;
import com.afcgym.platform.persistance.repositories.IUserRepository;
import com.afcgym.platform.service.dto.LoginResult;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

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
    void givenValidCredentials_whenLogin_thenReturnTokenAndRole() {
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(activeUser));
        when(passwordEncoder.matches("Secret123!", "hashed-password")).thenReturn(true);
        when(properties.jwtTtlMinutes()).thenReturn(60L);
        Jwt generatedJwt = Jwt.withTokenValue("token-value")
                .header("alg", "HS256")
                .claim("sub", "admin")
                .build();
        when(jwtEncoder.encode(any(JwtEncoderParameters.class))).thenReturn(generatedJwt);

        LoginResult result = authService.login("admin", "Secret123!");

        assertEquals("token-value", result.token());
        assertEquals(Role.ADMIN, result.role());
    }

    @Test
    void givenAnUnknownUsername_whenLogin_thenThrowUnauthorizedException() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThrows(UnauthorizedException.class, () -> authService.login("ghost", "whatever"));

        verify(jwtEncoder, never()).encode(any());
    }

    @Test
    void givenAnInactiveUser_whenLogin_thenThrowUnauthorizedException() {
        UserEntity inactiveUser = UserEntity.builder()
                .id(2L)
                .username("inactive")
                .fullName("Inactive User")
                .email("inactive@afcgym.com")
                .passwordHash("hashed-password")
                .role(Role.ADMIN)
                .active(false)
                .createdAt(Instant.now())
                .build();
        when(userRepository.findByUsername("inactive")).thenReturn(Optional.of(inactiveUser));

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
    void givenAToken_whenLogout_thenSaveItInTheBlocklist() {
        Jwt token = Jwt.withTokenValue("token-value")
                .header("alg", "HS256")
                .claim("sub", "admin")
                .claim("jti", "jti-123")
                .expiresAt(Instant.now().plusSeconds(3600))
                .issuedAt(Instant.now())
                .build();

        authService.logout(token);

        verify(blocklistRepository).save(any());
    }
}
