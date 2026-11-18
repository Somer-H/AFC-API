package com.afcgym.platform.web.controllers;

import static com.afcgym.platform.configuration.security.TestAuthentication.asRole;
import static com.afcgym.platform.configuration.security.TestAuthentication.asUser;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
class ProductsControllerTest {

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
    void givenASocioToken_whenCreateProduct_thenReturnForbidden() throws Exception {
        mockMvc.perform(post("/v1/products")
                        .with(asRole(Role.SOCIO))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"SKU-001\",\"name\":\"Whey Protein\",\"price\":850}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void givenAnAdminToken_whenCreateProduct_thenReturnCreatedWithDefaults() throws Exception {
        mockMvc.perform(post("/v1/products")
                        .with(asRole(Role.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"SKU-001\",\"name\":\"Whey Protein\",\"price\":850}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("SKU-001"))
                .andExpect(jsonPath("$.stock").value(0))
                .andExpect(jsonPath("$.minStock").value(5))
                .andExpect(jsonPath("$.lowStock").value(true))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void givenADuplicateCode_whenCreateProduct_thenReturnConflict() throws Exception {
        createProduct("SKU-001", "Whey Protein", 10);

        mockMvc.perform(post("/v1/products")
                        .with(asRole(Role.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"SKU-001\",\"name\":\"Otro\",\"price\":100}"))
                .andExpect(status().isConflict());
    }

    @Test
    void givenAnUnknownCategory_whenCreateProduct_thenReturnNotFound() throws Exception {
        mockMvc.perform(post("/v1/products")
                        .with(asRole(Role.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"SKU-002\",\"name\":\"Creatina\",\"price\":400,\"categoryId\":999999}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void givenAMissingPrice_whenCreateProduct_thenReturnBadRequest() throws Exception {
        mockMvc.perform(post("/v1/products")
                        .with(asRole(Role.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"SKU-003\",\"name\":\"Creatina\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void givenProducts_whenSearchLowStock_thenReturnOnlyTheLowStockOnes() throws Exception {
        createProduct("SKU-LOW", "Barra proteica", 2);
        createProduct("SKU-OK", "Shaker", 50);

        mockMvc.perform(get("/v1/products").param("lowStock", "true").with(asRole(Role.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].code").value("SKU-LOW"));
    }

    @Test
    void givenProducts_whenSearchByName_thenReturnMatchingOnes() throws Exception {
        createProduct("SKU-A", "Whey Protein", 10);
        createProduct("SKU-B", "Shaker", 10);

        mockMvc.perform(get("/v1/products").param("search", "whey").with(asRole(Role.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Whey Protein"));
    }

    @Test
    void givenAProduct_whenUpdate_thenReturnTheUpdatedData() throws Exception {
        long productId = createProduct("SKU-001", "Whey Protein", 10);

        mockMvc.perform(put("/v1/products/" + productId)
                        .with(asRole(Role.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Whey Protein 5kg\",\"price\":1800,\"minStock\":3}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Whey Protein 5kg"))
                .andExpect(jsonPath("$.minStock").value(3));
    }

    @Test
    void givenAProduct_whenDeactivate_thenItBecomesInactive() throws Exception {
        long productId = createProduct("SKU-001", "Whey Protein", 10);

        mockMvc.perform(patch("/v1/products/" + productId + "/deactivate").with(asRole(Role.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    void givenAProduct_whenRegisterPurchase_thenStockIncreasesAndTheMovementIsListed() throws Exception {
        long productId = createProduct("SKU-001", "Whey Protein", 10);
        long adminId = jdbc.queryForObject("SELECT id FROM app_user WHERE username = 'admin'", Long.class);

        mockMvc.perform(post("/v1/products/" + productId + "/inventory-movements")
                        .with(asUser(Role.ADMIN, adminId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\":20,\"unitCost\":40}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("PURCHASE"))
                .andExpect(jsonPath("$.finalStock").value(30))
                .andExpect(jsonPath("$.userId").value(adminId));

        mockMvc.perform(get("/v1/products/" + productId).with(asRole(Role.ADMIN)))
                .andExpect(jsonPath("$.stock").value(30));

        mockMvc.perform(get("/v1/products/" + productId + "/inventory-movements").with(asRole(Role.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void givenAZeroQuantity_whenRegisterPurchase_thenReturnBadRequest() throws Exception {
        long productId = createProduct("SKU-001", "Whey Protein", 10);

        mockMvc.perform(post("/v1/products/" + productId + "/inventory-movements")
                        .with(asRole(Role.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\":0}"))
                .andExpect(status().isBadRequest());
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
