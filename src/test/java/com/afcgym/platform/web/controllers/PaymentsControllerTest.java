package com.afcgym.platform.web.controllers;

import static com.afcgym.platform.configuration.security.TestAuthentication.asRole;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PaymentsControllerTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private JdbcTemplate jdbc;

    private MockMvc mockMvc;

    private long memberId;

    @BeforeEach
    void setUp() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
        memberId = createMember();
    }

    @Test
    void givenNoToken_whenRenew_thenReturnUnauthorized() throws Exception {
        String body = "{\"membershipType\":\"MONTHLY\",\"method\":\"CARD\",\"cardLastFour\":\"1234\"}";

        mockMvc.perform(post("/v1/members/" + memberId + "/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void givenASocioToken_whenRenew_thenReturnForbidden() throws Exception {
        String body = "{\"membershipType\":\"MONTHLY\",\"method\":\"CARD\",\"cardLastFour\":\"1234\"}";

        mockMvc.perform(post("/v1/members/" + memberId + "/payments")
                        .with(asRole(Role.SOCIO))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    void givenACardPayment_whenRenew_thenReturnCreatedAndApproved() throws Exception {
        String body = "{\"membershipType\":\"MONTHLY\",\"method\":\"CARD\",\"cardLastFour\":\"1234\"}";

        mockMvc.perform(post("/v1/members/" + memberId + "/payments")
                        .with(asRole(Role.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.amount").value(500.0))
                .andExpect(jsonPath("$.discountPercent").value(0));

        mockMvc.perform(get("/v1/members/" + memberId).with(asRole(Role.ADMIN)))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void givenACardPaymentWithoutLastFourDigits_whenRenew_thenReturnBadRequest() throws Exception {
        String body = "{\"membershipType\":\"MONTHLY\",\"method\":\"CARD\"}";

        mockMvc.perform(post("/v1/members/" + memberId + "/payments")
                        .with(asRole(Role.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void givenASpeiPayment_whenRenewThenApprove_thenMembershipGetsExtended() throws Exception {
        jdbc.update("INSERT INTO member (code, full_name, membership_type, due_date) "
                + "VALUES ('SOC-EXPIRED2', 'Expired Member', 'MONTHLY', CURRENT_DATE - 10)");
        long expiredMemberId = jdbc.queryForObject("SELECT id FROM member WHERE code = 'SOC-EXPIRED2'", Long.class);
        String renewBody = "{\"membershipType\":\"MONTHLY\",\"method\":\"SPEI\"}";
        String response = mockMvc.perform(post("/v1/members/" + expiredMemberId + "/payments")
                        .with(asRole(Role.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(renewBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING_BANK_VALIDATION"))
                .andExpect(jsonPath("$.reference").isNotEmpty())
                .andReturn()
                .getResponse()
                .getContentAsString();
        long paymentId = ((Number) JsonPath.read(response, "$.id")).longValue();

        mockMvc.perform(get("/v1/members/" + expiredMemberId).with(asRole(Role.ADMIN)))
                .andExpect(jsonPath("$.status").value("EXPIRED"));

        mockMvc.perform(patch("/v1/payments/" + paymentId + "/approve").with(asRole(Role.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));

        mockMvc.perform(get("/v1/members/" + expiredMemberId).with(asRole(Role.ADMIN)))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void givenATransferWhoseDepositNeverArrived_whenReject_thenItIsRejectedWithTheReason() throws Exception {
        jdbc.update("INSERT INTO member (code, full_name, membership_type, due_date) "
                + "VALUES ('SOC-REJECT1', 'Rejected Transfer', 'MONTHLY', CURRENT_DATE + 3)");
        long memberId = jdbc.queryForObject("SELECT id FROM member WHERE code = 'SOC-REJECT1'", Long.class);
        String response = mockMvc.perform(post("/v1/members/" + memberId + "/payments")
                        .with(asRole(Role.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"membershipType\":\"MONTHLY\",\"method\":\"SPEI\"}"))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        long paymentId = ((Number) JsonPath.read(response, "$.id")).longValue();

        mockMvc.perform(patch("/v1/payments/" + paymentId + "/reject")
                        .with(asRole(Role.SOCIO))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"No aparece en la banca\"}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(patch("/v1/payments/" + paymentId + "/reject")
                        .with(asRole(Role.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"No aparece en la banca\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"))
                .andExpect(jsonPath("$.rejectionReason").value("No aparece en la banca"))
                .andExpect(jsonPath("$.rejectedAt").isNotEmpty());
    }

    @Test
    void givenAnAlreadyApprovedPayment_whenApproveAgain_thenReturnBadRequest() throws Exception {
        String body = "{\"membershipType\":\"MONTHLY\",\"method\":\"CARD\",\"cardLastFour\":\"1234\"}";
        String response = mockMvc.perform(post("/v1/members/" + memberId + "/payments")
                        .with(asRole(Role.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andReturn()
                .getResponse()
                .getContentAsString();
        long paymentId = ((Number) JsonPath.read(response, "$.id")).longValue();

        mockMvc.perform(patch("/v1/payments/" + paymentId + "/approve").with(asRole(Role.ADMIN)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void givenAnUnknownPayment_whenApprove_thenReturnNotFound() throws Exception {
        mockMvc.perform(patch("/v1/payments/999999/approve").with(asRole(Role.ADMIN)))
                .andExpect(status().isNotFound());
    }

    @Test
    void givenTwoPayments_whenListByMember_thenReturnThemNewestFirst() throws Exception {
        mockMvc.perform(post("/v1/members/" + memberId + "/payments")
                .with(asRole(Role.ADMIN))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"membershipType\":\"MONTHLY\",\"method\":\"CARD\",\"cardLastFour\":\"1234\"}"));
        mockMvc.perform(post("/v1/members/" + memberId + "/payments")
                .with(asRole(Role.ADMIN))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"membershipType\":\"YEARLY\",\"method\":\"CARD\",\"cardLastFour\":\"4321\"}"));

        mockMvc.perform(get("/v1/members/" + memberId + "/payments").with(asRole(Role.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].membershipType").value("YEARLY"));
    }

    private long createMember() throws Exception {
        String body = "{\"fullName\":\"Ana Test\",\"membershipType\":\"MONTHLY\"}";
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
