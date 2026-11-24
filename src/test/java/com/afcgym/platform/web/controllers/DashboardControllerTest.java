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
import java.time.LocalDate;
import java.time.YearMonth;
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
class DashboardControllerTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private JdbcTemplate jdbc;

    private MockMvc mockMvc;

    private LocalDate today;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
        today = LocalDate.now();
    }

    @Test
    void givenASocioToken_whenSummary_thenReturnForbidden() throws Exception {
        mockMvc.perform(get("/v1/dashboard/summary").with(asRole(Role.SOCIO)))
                .andExpect(status().isForbidden());
    }

    @Test
    void givenAWindowOutOfRange_whenSummary_thenReturnBadRequest() throws Exception {
        mockMvc.perform(get("/v1/dashboard/summary").param("expiringWithinDays", "0").with(asRole(Role.ADMIN)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void givenRealMembersSalesAndPayments_whenSummary_thenEveryNumberComesFromThem() throws Exception {
        long payingMember = insertMember("SOC-DASH-1", "Ana Activa", today.plusDays(20), true);
        insertMember("SOC-DASH-2", "Beto Por Vencer", today.plusDays(2), true);
        insertMember("SOC-DASH-3", "Carla Vencida", today.minusDays(10), true);
        insertMember("SOC-DASH-4", "Dani Por Depurar", today.minusDays(90), true);
        insertMember("SOC-DASH-5", "Eli De Baja", today.plusDays(20), false);

        long categoryId = readId(mockMvc.perform(post("/v1/categories")
                        .with(asRole(Role.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Proteínas\"}"))
                .andReturn().getResponse().getContentAsString());
        long whey = createProduct("{\"code\":\"SKU-WHEY\",\"name\":\"Whey Protein\",\"price\":100,\"stock\":3,"
                + "\"minStock\":5,\"categoryId\":" + categoryId + "}");
        long shaker = createProduct("{\"code\":\"SKU-SHAKER\",\"name\":\"Shaker\",\"price\":100,\"stock\":50}");
        long discontinued = createProduct("{\"code\":\"SKU-OLD\",\"name\":\"Barra vieja\",\"price\":10,\"stock\":0}");
        mockMvc.perform(patch("/v1/products/" + discontinued + "/deactivate").with(asRole(Role.ADMIN)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/v1/sales")
                        .with(asRole(Role.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"items\":[{\"productId\":" + whey + ",\"quantity\":2},{\"productId\":" + shaker
                                + ",\"quantity\":1}],\"paymentMethod\":\"CASH\"}"))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/v1/members/" + payingMember + "/payments")
                        .with(asRole(Role.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"membershipType\":\"MONTHLY\",\"method\":\"CARD\",\"cardLastFour\":\"4242\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/v1/dashboard/summary").with(asRole(Role.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.date").value(today.toString()))
                .andExpect(jsonPath("$.members.active").value(2))
                .andExpect(jsonPath("$.members.expired").value(1))
                .andExpect(jsonPath("$.members.toPurge").value(1))
                .andExpect(jsonPath("$.members.newThisMonth").value(5))
                .andExpect(jsonPath("$.income.month").value(YearMonth.from(today).toString()))
                .andExpect(jsonPath("$.income.membershipIncome").value(500.0))
                .andExpect(jsonPath("$.income.storeIncome").value(300.0))
                .andExpect(jsonPath("$.income.total").value(800.0))
                .andExpect(jsonPath("$.monthlyIncome.length()").value(6))
                .andExpect(jsonPath("$.monthlyIncome[5].total").value(800.0))
                .andExpect(jsonPath("$.storeSalesByCategory[0].category").value("Proteínas"))
                .andExpect(jsonPath("$.storeSalesByCategory[0].amount").value(200.0))
                .andExpect(jsonPath("$.storeSalesByCategory[0].units").value(2))
                .andExpect(jsonPath("$.storeSalesByCategory[1].category").value("Sin categoría"))
                .andExpect(jsonPath("$.alerts.expiringWithinDays").value(3))
                .andExpect(jsonPath("$.alerts.expiringMemberships.length()").value(1))
                .andExpect(jsonPath("$.alerts.expiringMemberships[0].fullName").value("Beto Por Vencer"))
                .andExpect(jsonPath("$.alerts.expiringMemberships[0].daysLeft").value(2))
                .andExpect(jsonPath("$.alerts.lowStockProducts.length()").value(1))
                .andExpect(jsonPath("$.alerts.lowStockProducts[0].name").value("Whey Protein"))
                .andExpect(jsonPath("$.alerts.lowStockProducts[0].stock").value(1));
    }

    private long insertMember(String code, String fullName, LocalDate dueDate, boolean active) {
        jdbc.update("INSERT INTO member (code, full_name, phone, membership_type, due_date, active) "
                + "VALUES (?, ?, '5512345678', 'MONTHLY', ?, ?)", code, fullName, dueDate, active);
        return jdbc.queryForObject("SELECT id FROM member WHERE code = ?", Long.class, code);
    }

    private long createProduct(String body) throws Exception {
        return readId(mockMvc.perform(post("/v1/products")
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
