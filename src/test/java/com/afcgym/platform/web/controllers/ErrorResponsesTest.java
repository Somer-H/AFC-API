package com.afcgym.platform.web.controllers;

import static com.afcgym.platform.configuration.security.TestAuthentication.asRole;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ErrorResponsesTest {

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
    void givenAFormWithEmptyRequiredFields_whenSubmitted_thenSayWhichFieldsAreMissing() throws Exception {
        createMember("{\"fullName\":\"  \"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Faltan campos obligatorios: fullName, membershipType"))
                .andExpect(jsonPath("$.fields.fullName").value("es obligatorio"))
                .andExpect(jsonPath("$.fields.membershipType").value("es obligatorio"));
    }

    @Test
    void givenAFieldWithTheWrongFormat_whenSubmitted_thenNameTheFieldAndTheProblem() throws Exception {
        createMember("{\"fullName\":\"Ana\",\"email\":\"no-es-correo\",\"membershipType\":\"MONTHLY\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Revisa estos campos: email (no es un correo válido)"));
    }

    @Test
    void givenAnUnknownOption_whenSubmitted_thenListTheAllowedValues() throws Exception {
        createMember("{\"fullName\":\"Ana\",\"membershipType\":\"DAILY\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("El valor 'DAILY' no es válido para membershipType. "
                        + "Valores permitidos: WEEKLY, BIWEEKLY, MONTHLY, QUARTERLY, YEARLY"));
    }

    @Test
    void givenABodyThatIsNotJson_whenSubmitted_thenAnswerAClearBadRequest() throws Exception {
        createMember("{fullName: Ana")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("El cuerpo de la petición no es un JSON válido"));
    }

    @Test
    void givenAnIdThatIsNotANumber_whenRequested_thenNameTheParameter() throws Exception {
        mockMvc.perform(get("/v1/members/abc").with(asRole(Role.ADMIN)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("El parámetro id no tiene un formato válido"));
    }

    @Test
    void givenAMissingRequiredParameter_whenRequested_thenNameIt() throws Exception {
        mockMvc.perform(get("/v1/cash-cuts").param("category", "ALL").with(asRole(Role.ADMIN)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Falta el parámetro obligatorio period"));
    }

    @Test
    void givenARouteThatDoesNotExist_whenRequested_thenAnswerNotFoundInTheSameFormat() throws Exception {
        mockMvc.perform(get("/v1/this-route-does-not-exist").with(asRole(Role.ADMIN)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("La ruta solicitada no existe"));
    }

    @Test
    void givenAMethodTheRouteDoesNotSupport_whenRequested_thenAnswerMethodNotAllowed() throws Exception {
        mockMvc.perform(delete("/v1/members").with(asRole(Role.ADMIN)))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.message").value("Esta operación no está disponible en esta ruta"));
    }

    @Test
    void givenABodyThatIsNotSentAsJson_whenSubmitted_thenAskForJson() throws Exception {
        mockMvc.perform(post("/v1/members")
                        .with(asRole(Role.ADMIN))
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("Ana"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.message").value("El contenido debe enviarse como JSON"));
    }

    private ResultActions createMember(String body) throws Exception {
        return mockMvc.perform(post("/v1/members")
                .with(asRole(Role.ADMIN))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }
}
