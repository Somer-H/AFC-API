package com.afcgym.platform.web.controllers;

import static com.afcgym.platform.configuration.security.TestAuthentication.asRole;
import static com.afcgym.platform.configuration.security.TestAuthentication.asUser;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class SalesControllerTest {

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
    void givenASocioToken_whenRegisterSale_thenReturnForbidden() throws Exception {
        mockMvc.perform(post("/v1/sales")
                        .with(asRole(Role.SOCIO))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"items\":[{\"productId\":1,\"quantity\":1}],\"paymentMethod\":\"CASH\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void givenEnoughStock_whenRegisterSale_thenReturnTheTicketAndDiscountTheStock() throws Exception {
        long wheyId = createProduct("SKU-WHEY", "Whey Protein", 10);
        long shakerId = createProduct("SKU-SHAKER", "Shaker", 5);
        long adminId = jdbc.queryForObject("SELECT id FROM app_user WHERE username = 'admin'", Long.class);

        mockMvc.perform(post("/v1/sales")
                        .with(asUser(Role.ADMIN, adminId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(saleBody(wheyId, 2, shakerId, 1, "CASH")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.folio", startsWith("V-")))
                .andExpect(jsonPath("$.total").value(300.0))
                .andExpect(jsonPath("$.paymentMethod").value("CASH"))
                .andExpect(jsonPath("$.cashierId").value(adminId))
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.items[0].productName").value("Whey Protein"))
                .andExpect(jsonPath("$.items[0].subtotal").value(200.0));

        mockMvc.perform(get("/v1/products/" + wheyId).with(asRole(Role.ADMIN)))
                .andExpect(jsonPath("$.stock").value(8));

        mockMvc.perform(get("/v1/products/" + shakerId + "/inventory-movements").with(asRole(Role.ADMIN)))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].type").value("SALE"))
                .andExpect(jsonPath("$[0].finalStock").value(4));
    }

    @Test
    void givenMoreUnitsThanTheStock_whenRegisterSale_thenReturnConflictWithTheAvailableStock() throws Exception {
        long wheyId = createProduct("SKU-WHEY", "Whey Protein", 2);

        mockMvc.perform(post("/v1/sales")
                        .with(asRole(Role.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"items\":[{\"productId\":" + wheyId + ",\"quantity\":5}],\"paymentMethod\":\"CASH\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Stock insuficiente para Whey Protein: disponible 2"));

        mockMvc.perform(get("/v1/products/" + wheyId).with(asRole(Role.ADMIN)))
                .andExpect(jsonPath("$.stock").value(2));
    }

    @Test
    void givenAnEmptyCart_whenRegisterSale_thenReturnBadRequest() throws Exception {
        mockMvc.perform(post("/v1/sales")
                        .with(asRole(Role.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"items\":[],\"paymentMethod\":\"CASH\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void givenAnUnknownPaymentMethod_whenRegisterSale_thenReturnBadRequest() throws Exception {
        long wheyId = createProduct("SKU-WHEY", "Whey Protein", 10);

        mockMvc.perform(post("/v1/sales")
                        .with(asRole(Role.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"items\":[{\"productId\":" + wheyId + ",\"quantity\":1}],\"paymentMethod\":\"BITCOIN\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void givenACartWithinTheStock_whenValidateStock_thenReturnNoContent() throws Exception {
        long wheyId = createProduct("SKU-WHEY", "Whey Protein", 3);

        mockMvc.perform(post("/v1/sales/stock-validation")
                        .with(asRole(Role.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"items\":[{\"productId\":" + wheyId + ",\"quantity\":3}]}"))
                .andExpect(status().isNoContent());
    }

    @Test
    void givenACartAboveTheStock_whenValidateStock_thenReturnConflict() throws Exception {
        long wheyId = createProduct("SKU-WHEY", "Whey Protein", 3);

        mockMvc.perform(post("/v1/sales/stock-validation")
                        .with(asRole(Role.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"items\":[{\"productId\":" + wheyId + ",\"quantity\":4}]}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Stock insuficiente para Whey Protein: disponible 3"));
    }

    @Test
    void givenARegisteredSale_whenListByTodaysRange_thenReturnItWithItsItems() throws Exception {
        long wheyId = createProduct("SKU-WHEY", "Whey Protein", 10);
        long shakerId = createProduct("SKU-SHAKER", "Shaker", 5);
        registerSale(saleBody(wheyId, 1, shakerId, 1, "CARD"));
        String today = LocalDate.now().toString();

        mockMvc.perform(get("/v1/sales").param("from", today).param("to", today).with(asRole(Role.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].paymentMethod").value("CARD"))
                .andExpect(jsonPath("$[0].items.length()").value(2));

        String yesterday = LocalDate.now().minusDays(1).toString();
        mockMvc.perform(get("/v1/sales").param("from", yesterday).param("to", yesterday).with(asRole(Role.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void givenNoDateRange_whenListSales_thenReturnBadRequest() throws Exception {
        mockMvc.perform(get("/v1/sales").with(asRole(Role.ADMIN)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void givenAStartAfterTheEnd_whenListSales_thenReturnBadRequest() throws Exception {
        mockMvc.perform(get("/v1/sales")
                        .param("from", "2026-11-20")
                        .param("to", "2026-11-10")
                        .with(asRole(Role.ADMIN)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("La fecha inicial no puede ser posterior a la final"));
    }

    @Test
    void givenARegisteredSale_whenExportTodaysRange_thenDownloadAnExcelNamedAfterTheRange() throws Exception {
        long wheyId = createProduct("SKU-WHEY", "Whey Protein", 10);
        long shakerId = createProduct("SKU-SHAKER", "Shaker", 5);
        registerSale(saleBody(wheyId, 1, shakerId, 1, "CARD"));
        String today = LocalDate.now().toString();

        mockMvc.perform(get("/v1/sales/export").param("from", today).param("to", today).with(asRole(Role.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .andExpect(header().string("Content-Disposition",
                        containsString("ventas_" + today + "_" + today + ".xlsx")));
    }

    @Test
    void givenASocioToken_whenExportSales_thenReturnForbidden() throws Exception {
        mockMvc.perform(get("/v1/sales/export")
                        .param("from", "2026-11-01")
                        .param("to", "2026-11-30")
                        .with(asRole(Role.SOCIO)))
                .andExpect(status().isForbidden());
    }

    @Test
    void givenNoEndDate_whenExportSales_thenReturnBadRequestNamingTheParameter() throws Exception {
        mockMvc.perform(get("/v1/sales/export").param("from", "2026-11-01").with(asRole(Role.ADMIN)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Falta el parámetro obligatorio to"));
    }

    @Test
    void givenARegisteredSale_whenGetById_thenReturnItWithItsItems() throws Exception {
        long wheyId = createProduct("SKU-WHEY", "Whey Protein", 10);
        long shakerId = createProduct("SKU-SHAKER", "Shaker", 5);
        long saleId = registerSale(saleBody(wheyId, 1, shakerId, 2, "SPEI"));

        mockMvc.perform(get("/v1/sales/" + saleId).with(asRole(Role.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.folio").value(String.format("V-%06d", saleId)))
                .andExpect(jsonPath("$.total").value(300.0))
                .andExpect(jsonPath("$.items[1].quantity").value(2));
    }

    @Test
    void givenAnUnknownId_whenGetSale_thenReturnNotFound() throws Exception {
        mockMvc.perform(get("/v1/sales/999999").with(asRole(Role.ADMIN)))
                .andExpect(status().isNotFound());
    }

    private String saleBody(long firstId, int firstQuantity, long secondId, int secondQuantity, String method) {
        return "{\"items\":["
                + "{\"productId\":" + firstId + ",\"quantity\":" + firstQuantity + "},"
                + "{\"productId\":" + secondId + ",\"quantity\":" + secondQuantity + "}"
                + "],\"paymentMethod\":\"" + method + "\"}";
    }

    private long registerSale(String body) throws Exception {
        String response = mockMvc.perform(post("/v1/sales")
                        .with(asRole(Role.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andReturn()
                .getResponse()
                .getContentAsString();
        return ((Number) JsonPath.read(response, "$.id")).longValue();
    }

    private long createProduct(String code, String name, int stock) throws Exception {
        String body = "{\"code\":\"" + code + "\",\"name\":\"" + name + "\",\"price\":100,\"stock\":" + stock + "}";
        String response = mockMvc.perform(post("/v1/products")
                        .with(asRole(Role.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andReturn()
                .getResponse()
                .getContentAsString();
        return ((Number) JsonPath.read(response, "$.id")).longValue();
    }
}
