package com.afcgym.platform.web.controllers;

import static com.afcgym.platform.configuration.security.TestAuthentication.asRole;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class SuppliersControllerTest {

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
    void givenASocioToken_whenCreateSupplier_thenReturnForbidden() throws Exception {
        mockMvc.perform(post("/v1/suppliers")
                        .with(asRole(Role.SOCIO))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Proveedor Uno\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void givenAnAdminToken_whenCreateAndListSuppliers_thenReturnThem() throws Exception {
        mockMvc.perform(post("/v1/suppliers")
                        .with(asRole(Role.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Proveedor Uno\",\"contact\":\"ventas@proveedor.com\",\"phone\":\"5512345678\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Proveedor Uno"));

        mockMvc.perform(get("/v1/suppliers").with(asRole(Role.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void givenABlankName_whenCreateSupplier_thenReturnBadRequest() throws Exception {
        mockMvc.perform(post("/v1/suppliers")
                        .with(asRole(Role.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest());
    }
}
