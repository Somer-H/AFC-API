package com.afcgym.platform.configuration.security.jwt;

import com.afcgym.platform.persistance.repositories.IJwtBlocklistRepository;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RevokedTokenValidator implements OAuth2TokenValidator<Jwt> {

    private final IJwtBlocklistRepository blocklistRepository;

    @Override
    public OAuth2TokenValidatorResult validate(Jwt token) {
        List<String> identifiers = Stream.of(token.getId(), token.getClaimAsString(TokenClaims.SESSION_ID))
                .filter(Objects::nonNull)
                .toList();
        if (!identifiers.isEmpty() && blocklistRepository.existsByJtiIn(identifiers)) {
            return OAuth2TokenValidatorResult.failure(
                    new OAuth2Error("invalid_token", "El token fue invalidado por un cierre de sesión", null));
        }
        return OAuth2TokenValidatorResult.success();
    }
}
