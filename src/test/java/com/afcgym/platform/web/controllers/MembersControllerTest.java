package com.afcgym.platform.web.controllers;

import static com.afcgym.platform.configuration.security.TestAuthentication.asRole;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.afcgym.platform.persistance.entities.enums.Role;
import com.jayway.jsonpath.JsonPath;
import org.hamcrest.Matchers;
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
class MembersControllerTest {

    private static final String NEW_MEMBER_BODY =
            "{\"fullName\":\"Ana Test\",\"phone\":\"5512345678\",\"membershipType\":\"MONTHLY\"}";

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
    void givenNoToken_whenCreateMember_thenReturnUnauthorized() throws Exception {
        mockMvc.perform(post("/v1/members").contentType(MediaType.APPLICATION_JSON).content(NEW_MEMBER_BODY))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void givenASocioToken_whenCreateMember_thenReturnForbidden() throws Exception {
        mockMvc.perform(post("/v1/members")
                        .with(asRole(Role.SOCIO))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(NEW_MEMBER_BODY))
                .andExpect(status().isForbidden());
    }

    @Test
    void givenAnAdminToken_whenCreateMember_thenReturnCreatedWithAGeneratedCode() throws Exception {
        mockMvc.perform(post("/v1/members")
                        .with(asRole(Role.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(NEW_MEMBER_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.code").value(Matchers.startsWith("SOC-")))
                .andExpect(jsonPath("$.fullName").value("Ana Test"))
                .andExpect(jsonPath("$.membershipType").value("MONTHLY"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void givenAMissingMembershipType_whenCreateMember_thenReturnBadRequest() throws Exception {
        String body = "{\"fullName\":\"Ana Test\"}";

        mockMvc.perform(post("/v1/members")
                        .with(asRole(Role.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void givenMembers_whenSearchByName_thenReturnMatchingMembers() throws Exception {
        createMember("Carlos Lopez", "MONTHLY");
        createMember("Ana Test", "YEARLY");

        mockMvc.perform(get("/v1/members").param("search", "ana").with(asRole(Role.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].fullName").value("Ana Test"));
    }

    @Test
    void givenAnExpiredMember_whenSearchByStatusExpired_thenReturnIt() throws Exception {
        jdbc.update("INSERT INTO member (code, full_name, membership_type, due_date) "
                + "VALUES ('SOC-EXPIRED', 'Expired Member', 'MONTHLY', CURRENT_DATE - 10)");
        long expiredId = jdbc.queryForObject("SELECT id FROM member WHERE code = 'SOC-EXPIRED'", Long.class);

        mockMvc.perform(get("/v1/members").param("status", "EXPIRED").with(asRole(Role.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(expiredId));
    }

    @Test
    void givenAnExistingMember_whenGetById_thenReturnIt() throws Exception {
        long memberId = createMember("Ana Test", "MONTHLY");

        mockMvc.perform(get("/v1/members/" + memberId).with(asRole(Role.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(memberId));
    }

    @Test
    void givenAnUnknownId_whenGetById_thenReturnNotFound() throws Exception {
        mockMvc.perform(get("/v1/members/999999").with(asRole(Role.ADMIN)))
                .andExpect(status().isNotFound());
    }

    @Test
    void givenAnExistingMember_whenUpdate_thenReturnTheUpdatedData() throws Exception {
        long memberId = createMember("Ana Test", "MONTHLY");
        String body = "{\"fullName\":\"Ana Updated\",\"phone\":\"5500000000\",\"membershipType\":\"QUARTERLY\"}";

        mockMvc.perform(put("/v1/members/" + memberId)
                        .with(asRole(Role.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Ana Updated"))
                .andExpect(jsonPath("$.membershipType").value("QUARTERLY"));
    }

    @Test
    void givenAnExistingMember_whenDeactivate_thenItBecomesInactive() throws Exception {
        long memberId = createMember("Ana Test", "MONTHLY");

        mockMvc.perform(patch("/v1/members/" + memberId + "/deactivate").with(asRole(Role.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    void givenAPin_whenRegisterCredentials_thenAllowCheckInAfterwards() throws Exception {
        long memberId = createMember("Ana Test", "MONTHLY");
        String body = "{\"pin\":\"4321\"}";

        mockMvc.perform(patch("/v1/members/" + memberId + "/credentials")
                        .with(asRole(Role.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        String checkInBody = "{\"memberId\":" + memberId + ",\"pin\":\"4321\"}";
        mockMvc.perform(post("/v1/check-ins")
                        .with(asRole(Role.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(checkInBody))
                .andExpect(status().isCreated());
    }

    @Test
    void givenNeitherPinNorFingerprint_whenRegisterCredentials_thenReturnBadRequest() throws Exception {
        long memberId = createMember("Ana Test", "MONTHLY");

        mockMvc.perform(patch("/v1/members/" + memberId + "/credentials")
                        .with(asRole(Role.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    private long createMember(String fullName, String membershipType) throws Exception {
        String body = "{\"fullName\":\"" + fullName + "\",\"membershipType\":\"" + membershipType + "\"}";
        String response = mockMvc.perform(post("/v1/members")
                        .with(asRole(Role.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andReturn()
                .getResponse()
                .getContentAsString();
        return ((Number) JsonPath.read(response, "$.id")).longValue();
    }
}
