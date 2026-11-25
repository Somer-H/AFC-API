package com.afcgym.platform.web.controllers;

import static com.afcgym.platform.configuration.security.TestAuthentication.asRole;
import static com.afcgym.platform.configuration.security.TestAuthentication.asUser;
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
class AuditLogsControllerTest {

    private static final long AUDITED_ADMIN = 990001L;

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
    void givenAnAdminThatCreatesAMember_whenReadingTheAuditLog_thenSeeWhoWhenAndWhat() throws Exception {
        String created = mockMvc.perform(post("/v1/members")
                        .with(asUser(Role.ADMIN, AUDITED_ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"Ana Auditada\",\"phone\":\"5512345678\",\"membershipType\":\"MONTHLY\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long memberId = ((Number) JsonPath.read(created, "$.id")).longValue();

        mockMvc.perform(get("/v1/audit-logs").param("actorUserId", String.valueOf(AUDITED_ADMIN)).with(asRole(Role.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.items[0].action").value("Members.create"))
                .andExpect(jsonPath("$.items[0].actorRole").value("ADMIN"))
                .andExpect(jsonPath("$.items[0].httpMethod").value("POST"))
                .andExpect(jsonPath("$.items[0].path").value("/v1/members"))
                .andExpect(jsonPath("$.items[0].targetId").value(String.valueOf(memberId)))
                .andExpect(jsonPath("$.items[0].outcome").value("SUCCESS"))
                .andExpect(jsonPath("$.items[0].responseStatus").value(201))
                .andExpect(jsonPath("$.items[0].occurredAt").isNotEmpty());
    }

    @Test
    void givenASocioThatTriesToReadMemberRecords_whenReadingTheAuditLog_thenTheDeniedAttemptIsThere() throws Exception {
        long socioUser = createAccount(createMember("Beto Curioso"), "beto.curioso");
        long ownerUser = createAccount(createMember("Carla Dueña"), "carla.duena");
        String payment = mockMvc.perform(post("/v1/payments/me")
                        .with(asUser(Role.SOCIO, ownerUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"membershipType\":\"MONTHLY\",\"method\":\"SPEI\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long paymentId = ((Number) JsonPath.read(payment, "$.id")).longValue();

        mockMvc.perform(get("/v1/members").with(asUser(Role.SOCIO, socioUser)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/v1/payments/me/" + paymentId).with(asUser(Role.SOCIO, socioUser)))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/v1/audit-logs")
                        .param("actorUserId", String.valueOf(socioUser))
                        .param("outcome", "DENIED")
                        .with(asRole(Role.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.items[0].action").value("MemberPayments.getMine"))
                .andExpect(jsonPath("$.items[0].targetId").value(String.valueOf(paymentId)))
                .andExpect(jsonPath("$.items[0].responseStatus").value(404))
                .andExpect(jsonPath("$.items[1].action").value("AccessDenied"))
                .andExpect(jsonPath("$.items[1].path").value("/v1/members"))
                .andExpect(jsonPath("$.items[1].responseStatus").value(403));
    }

    @Test
    void givenASocioToken_whenReadingTheAuditLog_thenReturnForbidden() throws Exception {
        mockMvc.perform(get("/v1/audit-logs").with(asRole(Role.SOCIO)))
                .andExpect(status().isForbidden());
    }

    @Test
    void givenATooBigPage_whenReadingTheAuditLog_thenReturnBadRequest() throws Exception {
        mockMvc.perform(get("/v1/audit-logs").param("size", "500").with(asRole(Role.ADMIN)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("El tamaño de página debe estar entre 1 y 100"));
    }

    private long createMember(String fullName) throws Exception {
        String response = mockMvc.perform(post("/v1/members")
                        .with(asRole(Role.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"" + fullName + "\",\"phone\":\"5512345678\",\"membershipType\":\"MONTHLY\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(response, "$.id")).longValue();
    }

    private long createAccount(long memberId, String username) throws Exception {
        String body = "{\"username\":\"" + username + "\",\"email\":\"" + username + "@afcgym.com\","
                + "\"password\":\"Secret-123\"}";
        String response = mockMvc.perform(post("/v1/members/" + memberId + "/account")
                        .with(asRole(Role.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(response, "$.id")).longValue();
    }
}
