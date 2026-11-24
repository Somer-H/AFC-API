package com.afcgym.platform.web.controllers;

import static com.afcgym.platform.configuration.security.TestAuthentication.asRole;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.afcgym.platform.persistance.entities.enums.Role;
import com.jayway.jsonpath.JsonPath;
import java.time.LocalDate;
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
class CashCutsControllerTest {

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
    void givenASocioToken_whenGenerateCashCut_thenReturnForbidden() throws Exception {
        mockMvc.perform(get("/v1/cash-cuts")
                        .param("period", "DAILY")
                        .param("category", "ALL")
                        .with(asRole(Role.SOCIO)))
                .andExpect(status().isForbidden());
    }

    @Test
    void givenPaymentsAndSalesToday_whenGenerateDailyForAll_thenCountOnlyApprovedMoneyAndSplitItByMethod()
            throws Exception {
        registerTodaysMovements();

        mockMvc.perform(get("/v1/cash-cuts")
                        .param("period", "DAILY")
                        .param("category", "ALL")
                        .with(asRole(Role.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.period").value("DAILY"))
                .andExpect(jsonPath("$.from").value(LocalDate.now().toString()))
                .andExpect(jsonPath("$.membershipIncome").value(1000.0))
                .andExpect(jsonPath("$.storeIncome").value(200.0))
                .andExpect(jsonPath("$.totalIncome").value(1200.0))
                .andExpect(jsonPath("$.membershipPayments").value(2))
                .andExpect(jsonPath("$.storeSales").value(1))
                .andExpect(jsonPath("$.paymentMethods.length()").value(3))
                .andExpect(jsonPath("$.paymentMethods[0].method").value("CARD"))
                .andExpect(jsonPath("$.paymentMethods[0].amount").value(500.0))
                .andExpect(jsonPath("$.paymentMethods[0].percentage").value(41.67))
                .andExpect(jsonPath("$.paymentMethods[1].method").value("SPEI"))
                .andExpect(jsonPath("$.paymentMethods[1].amount").value(500.0))
                .andExpect(jsonPath("$.paymentMethods[2].method").value("CASH"))
                .andExpect(jsonPath("$.paymentMethods[2].amount").value(200.0))
                .andExpect(jsonPath("$.paymentMethods[2].percentage").value(16.67));
    }

    @Test
    void givenPaymentsAndSalesToday_whenGenerateForMemberships_thenLeaveTheStoreOut() throws Exception {
        registerTodaysMovements();

        mockMvc.perform(get("/v1/cash-cuts")
                        .param("period", "WEEKLY")
                        .param("category", "MEMBERSHIPS")
                        .with(asRole(Role.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.storeIncome").value(nullValue()))
                .andExpect(jsonPath("$.totalIncome").value(1000.0))
                .andExpect(jsonPath("$.paymentMethods[0].percentage").value(50.0))
                .andExpect(jsonPath("$.paymentMethods[2].amount").value(0.0));
    }

    @Test
    void givenPaymentsAndSalesToday_whenGenerateForTheStore_thenLeaveMembershipsOut() throws Exception {
        registerTodaysMovements();

        mockMvc.perform(get("/v1/cash-cuts")
                        .param("period", "MONTHLY")
                        .param("category", "STORE")
                        .with(asRole(Role.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.membershipIncome").value(nullValue()))
                .andExpect(jsonPath("$.totalIncome").value(200.0))
                .andExpect(jsonPath("$.paymentMethods[2].percentage").value(100.0));
    }

    @Test
    void givenNoCategory_whenGenerateCashCut_thenReturnBadRequest() throws Exception {
        mockMvc.perform(get("/v1/cash-cuts").param("period", "DAILY").with(asRole(Role.ADMIN)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void givenAnUnknownPeriod_whenGenerateCashCut_thenReturnBadRequest() throws Exception {
        mockMvc.perform(get("/v1/cash-cuts")
                        .param("period", "YEARLY")
                        .param("category", "ALL")
                        .with(asRole(Role.ADMIN)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void givenAFutureDate_whenGenerateCashCut_thenReturnBadRequest() throws Exception {
        mockMvc.perform(get("/v1/cash-cuts")
                        .param("period", "DAILY")
                        .param("category", "ALL")
                        .param("date", LocalDate.now().plusDays(1).toString())
                        .with(asRole(Role.ADMIN)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("La fecha del corte no puede ser futura"));
    }

    @Test
    void givenTheFilters_whenExport_thenDownloadAnXlsxNamedAfterThem() throws Exception {
        String today = LocalDate.now().toString();

        mockMvc.perform(get("/v1/cash-cuts/export")
                        .param("period", "DAILY")
                        .param("category", "ALL")
                        .with(asRole(Role.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .andExpect(header().string("Content-Disposition",
                        containsString("corte-de-caja_diario_todo_" + today + "_" + today + ".xlsx")));
    }

    @Test
    void givenASocioToken_whenExport_thenReturnForbidden() throws Exception {
        mockMvc.perform(get("/v1/cash-cuts/export")
                        .param("period", "DAILY")
                        .param("category", "ALL")
                        .with(asRole(Role.SOCIO)))
                .andExpect(status().isForbidden());
    }

    private void registerTodaysMovements() throws Exception {
        long memberId = createMember();
        pay(memberId, "{\"membershipType\":\"MONTHLY\",\"method\":\"CARD\",\"cardLastFour\":\"4242\"}");
        long speiPaymentId = pay(memberId, "{\"membershipType\":\"MONTHLY\",\"method\":\"SPEI\"}");
        mockMvc.perform(patch("/v1/payments/" + speiPaymentId + "/approve").with(asRole(Role.ADMIN)))
                .andExpect(status().isOk());
        pay(memberId, "{\"membershipType\":\"MONTHLY\",\"method\":\"SPEI\"}");

        long productId = readId(mockMvc.perform(post("/v1/products")
                        .with(asRole(Role.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"SKU-CUT\",\"name\":\"Barra proteica\",\"price\":100,\"stock\":10}"))
                .andReturn().getResponse().getContentAsString());
        mockMvc.perform(post("/v1/sales")
                        .with(asRole(Role.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"items\":[{\"productId\":" + productId + ",\"quantity\":2}],\"paymentMethod\":\"CASH\"}"))
                .andExpect(status().isCreated());
    }

    private long createMember() throws Exception {
        return readId(mockMvc.perform(post("/v1/members")
                        .with(asRole(Role.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"Ana Corte\",\"phone\":\"5512345678\",\"membershipType\":\"MONTHLY\"}"))
                .andReturn().getResponse().getContentAsString());
    }

    private long pay(long memberId, String body) throws Exception {
        return readId(mockMvc.perform(post("/v1/members/" + memberId + "/payments")
                        .with(asRole(Role.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString());
    }

    private long readId(String response) {
        return ((Number) JsonPath.read(response, "$.id")).longValue();
    }
}
