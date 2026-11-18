package com.afcgym.platform.service.impl;

import com.afcgym.platform.configuration.security.SecurityProperties;
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
        UserEntity authenticatedUser = user.get();
        Instant now = Instant.now();
        Instant expiresAt = now.plus(properties.jwtTtlMinutes(), ChronoUnit.MINUTES);
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .id(UUID.randomUUID().toString())
                .issuer(ISSUER)
                .subject(authenticatedUser.getUsername())
                .issuedAt(now)
                .expiresAt(expiresAt)
                .claim("userId", authenticatedUser.getId())
                .claim("role", authenticatedUser.getRole().name())
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new LoginResult(token, authenticatedUser.getRole(), ChronoUnit.SECONDS.between(now, expiresAt));
    }

    @Override
    @Transactional
    public void logout(Jwt token) {
        blocklistRepository.save(JwtBlocklistEntity.builder()
                .jti(token.getId())
                .expiresAt(token.getExpiresAt())
                .build());
    }
}
