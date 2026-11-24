package com.afcgym.platform.web.controllers;

import static com.afcgym.platform.configuration.security.TestAuthentication.asRole;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.afcgym.platform.persistance.entities.enums.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AuthControllerTest {

    private static final String LOGIN_BODY = "{\"username\":\"admin\",\"password\":\"Test-password-123\"}";

    @Autowired
    private WebApplicationContext webApplicationContext;

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
    void givenTheBootstrapAdmin_whenLogin_thenReturnTheTokenAndRole() throws Exception {
        mockMvc.perform(post("/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content(LOGIN_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andExpect(jsonPath("$.expiresInSeconds").isNumber());
    }

    @Test
    void givenAWrongPassword_whenLogin_thenReturnUnauthorized() throws Exception {
        String body = "{\"username\":\"admin\",\"password\":\"wrong-password\"}";

        mockMvc.perform(post("/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Usuario o contraseña incorrectos"));
    }

    @Test
    void givenBlankCredentials_whenLogin_thenReturnBadRequest() throws Exception {
        String body = "{\"username\":\"\",\"password\":\"\"}";

        mockMvc.perform(post("/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void givenNoToken_whenLogout_thenReturnUnauthorized() throws Exception {
        mockMvc.perform(post("/v1/auth/logout"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void givenAnAuthenticatedUser_whenLogout_thenInvalidateTheToken() throws Exception {
        mockMvc.perform(post("/v1/auth/logout").with(asRole(Role.SOCIO)))
                .andExpect(status().isNoContent());
    }

    @Test
    void givenAKnownEmail_whenRequestPasswordReset_thenReturnNoContent() throws Exception {
        String body = "{\"email\":\"admin@afcgym.local\"}";

        mockMvc.perform(post("/v1/auth/password-resets").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isNoContent());
    }

    @Test
    void givenAnUnknownEmail_whenRequestPasswordReset_thenReturnNoContent() throws Exception {
        String body = "{\"email\":\"ghost@afcgym.local\"}";

        mockMvc.perform(post("/v1/auth/password-resets").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isNoContent());
    }

    @Test
    void givenAValidResetToken_whenConfirmPasswordReset_thenAllowLoginWithTheNewPassword() throws Exception {
        long userId = jdbc.queryForObject("SELECT id FROM app_user WHERE username = 'admin'", Long.class);
        jdbc.update("INSERT INTO password_reset_token (user_id, token, expires_at) "
                + "VALUES (?, 'reset-token-123', DATEADD('MINUTE', 30, CURRENT_TIMESTAMP))", userId);
        String confirmBody = "{\"token\":\"reset-token-123\",\"newPassword\":\"NewSecret123!\"}";

        mockMvc.perform(post("/v1/auth/password-resets/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(confirmBody))
                .andExpect(status().isNoContent());

        String newLoginBody = "{\"username\":\"admin\",\"password\":\"NewSecret123!\"}";
        mockMvc.perform(post("/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content(newLoginBody))
                .andExpect(status().isOk());
    }

    @Test
    void givenAnUnknownResetToken_whenConfirmPasswordReset_thenReturnBadRequest() throws Exception {
        String body = "{\"token\":\"does-not-exist\",\"newPassword\":\"NewSecret123!\"}";

        mockMvc.perform(post("/v1/auth/password-resets/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("El enlace de recuperación no es válido o ya expiró"));
    }
}
