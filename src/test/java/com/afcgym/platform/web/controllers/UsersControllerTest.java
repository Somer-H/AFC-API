package com.afcgym.platform.web.controllers;

import static com.afcgym.platform.configuration.security.TestAuthentication.asRole;
import static com.afcgym.platform.configuration.security.TestAuthentication.asUser;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
class UsersControllerTest {

    private static final String NEW_ADMIN_BODY = "{\"username\":\"newadmin\",\"fullName\":\"New Admin\","
            + "\"email\":\"newadmin@afcgym.com\",\"password\":\"Secret123!\"}";

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
    void givenNoToken_whenCreateUser_thenReturnUnauthorized() throws Exception {
        mockMvc.perform(post("/v1/users").contentType(MediaType.APPLICATION_JSON).content(NEW_ADMIN_BODY))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void givenASocioToken_whenCreateUser_thenReturnForbidden() throws Exception {
        mockMvc.perform(post("/v1/users")
                        .with(asRole(Role.SOCIO))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(NEW_ADMIN_BODY))
                .andExpect(status().isForbidden());
    }

    @Test
    void givenAnAdminToken_whenCreateUser_thenReturnCreated() throws Exception {
        mockMvc.perform(post("/v1/users")
                        .with(asRole(Role.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(NEW_ADMIN_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.username").value("newadmin"))
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void givenAnExistingUsername_whenCreateUser_thenReturnConflict() throws Exception {
        String body = "{\"username\":\"admin\",\"fullName\":\"Other Admin\","
                + "\"email\":\"other@afcgym.com\",\"password\":\"Secret123!\"}";

        mockMvc.perform(post("/v1/users")
                        .with(asRole(Role.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict());
    }

    @Test
    void givenNoToken_whenGetMe_thenReturnUnauthorized() throws Exception {
        mockMvc.perform(get("/v1/users/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void givenTheBootstrapAdmin_whenGetMe_thenReturnOwnProfile() throws Exception {
        long adminId = jdbc.queryForObject("SELECT id FROM app_user WHERE username = 'admin'", Long.class);

        mockMvc.perform(get("/v1/users/me").with(asUser(Role.ADMIN, adminId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(adminId))
                .andExpect(jsonPath("$.username").value("admin"))
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }
}
