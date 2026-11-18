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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CheckInsControllerTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private MockMvc mockMvc;

    private long activeMemberId;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
        jdbc.update("INSERT INTO member (code, full_name, membership_type, due_date, pin_hash, active, created_at) "
                        + "VALUES ('SOC-TEST01', 'Ana Test', 'MONTHLY', DATEADD('DAY', 30, CURRENT_DATE), ?, TRUE, CURRENT_TIMESTAMP)",
                passwordEncoder.encode("1234"));
        activeMemberId = jdbc.queryForObject("SELECT MAX(id) FROM member", Long.class);
    }

    @Test
    void givenNoToken_whenCheckIn_thenReturnUnauthorized() throws Exception {
        String body = "{\"memberId\":" + activeMemberId + ",\"pin\":\"1234\"}";

        mockMvc.perform(post("/v1/check-ins").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void givenAValidPin_whenCheckIn_thenReturnCreated() throws Exception {
        String body = "{\"memberId\":" + activeMemberId + ",\"pin\":\"1234\"}";

        mockMvc.perform(post("/v1/check-ins")
                        .with(asRole(Role.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.memberId").value(activeMemberId))
                .andExpect(jsonPath("$.method").value("PIN"));
    }

    @Test
    void givenAWrongPin_whenCheckIn_thenReturnBadRequest() throws Exception {
        String body = "{\"memberId\":" + activeMemberId + ",\"pin\":\"0000\"}";

        mockMvc.perform(post("/v1/check-ins")
                        .with(asRole(Role.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("PIN o huella inválidos"));
    }

    @Test
    void givenAnUnknownMember_whenCheckIn_thenReturnNotFound() throws Exception {
        String body = "{\"memberId\":999999,\"pin\":\"1234\"}";

        mockMvc.perform(post("/v1/check-ins")
                        .with(asRole(Role.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound());
    }
}
