package com.afcgym.platform.web.controllers;

import static com.afcgym.platform.configuration.security.TestAuthentication.asRole;
import static com.afcgym.platform.configuration.security.TestAuthentication.asUser;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.afcgym.platform.persistance.entities.enums.Role;
import com.jayway.jsonpath.JsonPath;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
class MemberPaymentsControllerTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    private LocalDateTime tomorrowEvening;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
        tomorrowEvening = LocalDate.now().plusDays(1).atTime(18, 0);
    }

    @Test
    void givenASocio_whenStartATransfer_thenReturnTheBankDetailsAndLetThemFollowIt() throws Exception {
        long userId = createAccount(createMember("Ana Transferencia"), "ana.transferencia");

        String response = start(userId, "{\"membershipType\":\"MONTHLY\",\"method\":\"SPEI\"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING_BANK_VALIDATION"))
                .andExpect(jsonPath("$.amount").value(500.0))
                .andExpect(jsonPath("$.bankTransfer.clabe").value("000000000000000000"))
                .andExpect(jsonPath("$.bankTransfer.reference").value(startsWith("PAG-")))
                .andExpect(jsonPath("$.checkoutUrl").doesNotExist())
                .andReturn().getResponse().getContentAsString();
        long paymentId = readId(response);

        mockMvc.perform(get("/v1/payments/me").with(asUser(Role.SOCIO, userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(paymentId));
        mockMvc.perform(get("/v1/payments/me/" + paymentId).with(asUser(Role.SOCIO, userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bankTransfer.reference").value(String.format("PAG-%06d", paymentId)));
    }

    @Test
    void givenASocio_whenStartACashRenewalRescheduleAndCancelIt_thenTheAppointmentFollowsEachStep() throws Exception {
        long userId = createAccount(createMember("Beto Efectivo"), "beto.efectivo");

        String response = start(userId, cashBody(tomorrowEvening))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING_APPOINTMENT"))
                .andExpect(jsonPath("$.appointment.status").value("PENDING"))
                .andExpect(jsonPath("$.appointment.scheduledAt").value(iso(tomorrowEvening)))
                .andReturn().getResponse().getContentAsString();
        long paymentId = readId(response);

        mockMvc.perform(patch("/v1/payments/me/" + paymentId + "/appointment")
                        .with(asUser(Role.SOCIO, userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"appointmentAt\":\"" + tomorrowEvening.plusHours(1) + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.appointment.scheduledAt").value(iso(tomorrowEvening.plusHours(1))));

        mockMvc.perform(patch("/v1/payments/me/" + paymentId + "/cancel").with(asUser(Role.SOCIO, userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.appointment.status").value("CANCELLED"));
    }

    @Test
    void givenAPaymentOfAnotherMember_whenReadOrCancelIt_thenReturnNotFound() throws Exception {
        long ownerId = createAccount(createMember("Carla Dueña"), "carla.duena");
        long otherId = createAccount(createMember("Dani Curioso"), "dani.curioso");
        long paymentId = readId(start(ownerId, "{\"membershipType\":\"MONTHLY\",\"method\":\"SPEI\"}")
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString());

        mockMvc.perform(get("/v1/payments/me/" + paymentId).with(asUser(Role.SOCIO, otherId)))
                .andExpect(status().isNotFound());
        mockMvc.perform(patch("/v1/payments/me/" + paymentId + "/cancel").with(asUser(Role.SOCIO, otherId)))
                .andExpect(status().isNotFound());
    }

    @Test
    void givenMercadoPagoIsUnreachable_whenStartACardRenewal_thenReturnServiceUnavailable() throws Exception {
        long userId = createAccount(createMember("Eli Tarjeta"), "eli.tarjeta");

        start(userId, "{\"membershipType\":\"MONTHLY\",\"method\":\"CARD\"}")
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.message").value("No pudimos iniciar el pago con tarjeta. Intenta de nuevo en unos minutos"));
    }

    @Test
    void givenAnAdminToken_whenStart_thenReturnForbidden() throws Exception {
        mockMvc.perform(post("/v1/payments/me")
                        .with(asRole(Role.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"membershipType\":\"MONTHLY\",\"method\":\"SPEI\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void givenNoToken_whenListMine_thenReturnUnauthorized() throws Exception {
        mockMvc.perform(get("/v1/payments/me")).andExpect(status().isUnauthorized());
    }

    private ResultActions start(long userId, String body) throws Exception {
        return mockMvc.perform(post("/v1/payments/me")
                .with(asUser(Role.SOCIO, userId))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private String cashBody(LocalDateTime appointmentAt) {
        return "{\"membershipType\":\"MONTHLY\",\"method\":\"CASH\",\"appointmentAt\":\"" + appointmentAt + "\"}";
    }

    private String iso(LocalDateTime dateTime) {
        return dateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }

    private long createMember(String fullName) throws Exception {
        return readId(mockMvc.perform(post("/v1/members")
                        .with(asRole(Role.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"" + fullName + "\",\"phone\":\"5512345678\",\"membershipType\":\"MONTHLY\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString());
    }

    private long createAccount(long memberId, String username) throws Exception {
        String body = "{\"username\":\"" + username + "\",\"email\":\"" + username + "@afcgym.com\","
                + "\"password\":\"Secret-123\"}";
        return readId(mockMvc.perform(post("/v1/members/" + memberId + "/account")
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
