package com.afcgym.platform.configuration.security.jwt;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.afcgym.platform.persistance.repositories.IJwtBlocklistRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;

@ExtendWith(MockitoExtension.class)
class RevokedTokenValidatorTest {

    @Mock
    private IJwtBlocklistRepository blocklistRepository;

    @InjectMocks
    private RevokedTokenValidator validator;

    @Test
    void givenATokenOfAClosedSession_whenValidate_thenRejectIt() {
        when(blocklistRepository.existsByJtiIn(List.of("token-1", "session-1"))).thenReturn(true);

        assertTrue(validator.validate(token("token-1", "session-1")).hasErrors());
    }

    @Test
    void givenATokenOfAnOpenSession_whenValidate_thenAcceptIt() {
        when(blocklistRepository.existsByJtiIn(List.of("token-1", "session-1"))).thenReturn(false);

        assertFalse(validator.validate(token("token-1", "session-1")).hasErrors());
    }

    @Test
    void givenATokenWithoutSession_whenValidate_thenOnlyCheckItsOwnId() {
        when(blocklistRepository.existsByJtiIn(List.of("token-1"))).thenReturn(false);

        assertFalse(validator.validate(token("token-1", null)).hasErrors());
    }

    private Jwt token(String id, String sessionId) {
        Jwt.Builder builder = Jwt.withTokenValue("token-value")
                .header("alg", "HS256")
                .claim("jti", id);
        if (sessionId != null) {
            builder.claim(TokenClaims.SESSION_ID, sessionId);
        }
        return builder.build();
    }
}
