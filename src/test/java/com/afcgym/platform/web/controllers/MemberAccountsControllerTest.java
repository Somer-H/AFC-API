package com.afcgym.platform.web.controllers;

import static com.afcgym.platform.configuration.security.TestAuthentication.asRole;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.afcgym.platform.persistance.entities.enums.Role;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class MemberAccountsControllerTest {

    private static final String ACCOUNT =
            "{\"username\":\"ana.socia\",\"email\":\"ana.socia@afcgym.com\",\"password\":\"Secret-123\"}";

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
    }

    @Test
    void givenAMember_whenAdminCreatesTheAccount_thenTheMemberCanLogInAsSocio() throws Exception {
        long memberId = createMember();

        String response = mockMvc.perform(post("/v1/members/" + memberId + "/account")
                        .with(asRole(Role.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ACCOUNT))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("ana.socia"))
                .andExpect(jsonPath("$.fullName").value("Ana Socia"))
                .andExpect(jsonPath("$.role").value("SOCIO"))
                .andReturn().getResponse().getContentAsString();
        long userId = ((Number) JsonPath.read(response, "$.id")).longValue();

        mockMvc.perform(get("/v1/members/" + memberId).with(asRole(Role.ADMIN)))
                .andExpect(jsonPath("$.userId").value(userId));

        mockMvc.perform(post("/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"ana.socia\",\"password\":\"Secret-123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("SOCIO"));
    }

    @Test
    void givenAMemberWithAnAccount_whenCreateAnotherOne_thenReturnConflict() throws Exception {
        long memberId = createMember();
        mockMvc.perform(post("/v1/members/" + memberId + "/account")
                        .with(asRole(Role.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ACCOUNT))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/v1/members/" + memberId + "/account")
                        .with(asRole(Role.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"otra\",\"email\":\"otra@afcgym.com\",\"password\":\"Secret-123\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("El socio ya tiene una cuenta de acceso"));
    }

    @Test
    void givenAShortPassword_whenCreateAccount_thenReturnBadRequest() throws Exception {
        long memberId = createMember();

        mockMvc.perform(post("/v1/members/" + memberId + "/account")
                        .with(asRole(Role.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"ana\",\"email\":\"ana@afcgym.com\",\"password\":\"123\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void givenASocioToken_whenCreateAccount_thenReturnForbidden() throws Exception {
        mockMvc.perform(post("/v1/members/1/account")
                        .with(asRole(Role.SOCIO))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ACCOUNT))
                .andExpect(status().isForbidden());
    }

    @Test
    void givenAnAdminToken_whenCreateAndListCoaches_thenReturnThemByName() throws Exception {
        mockMvc.perform(post("/v1/coaches")
                        .with(asRole(Role.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Sofi Martínez\",\"avatarUrl\":\"https://example.com/sofi.png\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.avatarUrl").value("https://example.com/sofi.png"));
        mockMvc.perform(post("/v1/coaches")
                .with(asRole(Role.ADMIN))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Alex Rivera\"}"));

        mockMvc.perform(get("/v1/coaches").with(asRole(Role.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("Alex Rivera"));
    }

    private long createMember() throws Exception {
        String response = mockMvc.perform(post("/v1/members")
                        .with(asRole(Role.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"Ana Socia\",\"phone\":\"5512345678\",\"membershipType\":\"MONTHLY\"}"))
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(response, "$.id")).longValue();
    }
}
