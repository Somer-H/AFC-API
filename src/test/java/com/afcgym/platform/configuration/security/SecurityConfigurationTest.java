package com.afcgym.platform.configuration.security;

import static com.afcgym.platform.configuration.security.TestAuthentication.asRole;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.afcgym.platform.persistance.entities.enums.Role;
import com.jayway.jsonpath.JsonPath;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class SecurityConfigurationTest {

    private static final String UNAUTHORIZED_MESSAGE = "Autenticación requerida o token inválido";

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private JwtEncoder jwtEncoder;

    @Autowired
    private JdbcTemplate jdbc;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
    }

    @Test
    void givenNoToken_whenPostAProtectedEndpoint_thenReturnUnauthorized() throws Exception {
        mockMvc.perform(post("/v1/auth/logout"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value(UNAUTHORIZED_MESSAGE));
    }

    @Test
    void givenAMalformedToken_whenPostAProtectedEndpoint_thenReturnUnauthorized() throws Exception {
        mockMvc.perform(post("/v1/auth/logout").header(HttpHeaders.AUTHORIZATION, "Bearer not-a-valid-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value(UNAUTHORIZED_MESSAGE));
    }

    @Test
    void givenAnExpiredToken_whenPostAProtectedEndpoint_thenReturnUnauthorized() throws Exception {
        String token = buildToken(jwtEncoder, Instant.now().minus(2, ChronoUnit.HOURS), Instant.now().minus(1, ChronoUnit.HOURS));

        mockMvc.perform(post("/v1/auth/logout").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value(UNAUTHORIZED_MESSAGE));
    }

    @Test
    void givenATokenSignedWithAnotherSecret_whenPostAProtectedEndpoint_thenReturnUnauthorized() throws Exception {
        byte[] otherSecret = "another-secret-another-secret-another-123".getBytes(StandardCharsets.UTF_8);
        JwtEncoder otherEncoder = new NimbusJwtEncoder(new ImmutableSecret<>(new SecretKeySpec(otherSecret, "HmacSHA256")));
        String token = buildToken(otherEncoder, Instant.now(), Instant.now().plus(1, ChronoUnit.HOURS));

        mockMvc.perform(post("/v1/auth/logout").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value(UNAUTHORIZED_MESSAGE));
    }

    @ParameterizedTest
    @EnumSource(Role.class)
    void givenAnyRole_whenPostAProtectedEndpoint_thenReturnNoContent(Role role) throws Exception {
        mockMvc.perform(post("/v1/auth/logout").with(asRole(role)))
                .andExpect(status().isNoContent());
    }

    @Test
    void givenACoachAccount_whenLogin_thenItCanOnlyReadItsProfileAndKeepItsSession() throws Exception {
        jdbc.update("INSERT INTO app_user (username, full_name, email, password_hash, role) "
                + "SELECT 'coach1', 'Coach Uno', 'coach1@afcgym.com', password_hash, 'COACH' "
                + "FROM app_user WHERE username = 'admin'");
        String loginResponse = mockMvc.perform(post("/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"coach1\",\"password\":\"Test-password-123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("COACH"))
                .andReturn().getResponse().getContentAsString();
        String token = JsonPath.read(loginResponse, "$.token");

        mockMvc.perform(get("/v1/users/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("COACH"));
        mockMvc.perform(post("/v1/auth/refresh").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk());
        mockMvc.perform(get("/v1/members").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/v1/members", "/v1/products", "/v1/sales?from=2026-11-01&to=2026-11-30", "/v1/dashboard/summary",
            "/v1/announcements/active", "/v1/routines/by-day", "/v1/schedule-blocks/occupancy", "/v1/payments/me",
            "/v1/bookings/me", "/v1/audit-logs", "/v1/a-path-that-does-not-exist"})
    void givenACoachToken_whenGetAnyModule_thenReturnForbidden(String path) throws Exception {
        mockMvc.perform(get(path).with(asRole(Role.COACH)))
                .andExpect(status().isForbidden());
    }

    @Test
    void givenNoToken_whenGetTheHealthEndpoint_thenReturnOk() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    @Test
    void givenNoToken_whenGetTheApiDocs_thenReturnOk() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk());
    }

    private String buildToken(JwtEncoder encoder, Instant issuedAt, Instant expiresAt) {
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject("someone")
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .claim("role", Role.ADMIN.name())
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}
