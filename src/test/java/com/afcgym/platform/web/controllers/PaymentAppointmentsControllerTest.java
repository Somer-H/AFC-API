package com.afcgym.platform.web.controllers;

import static com.afcgym.platform.configuration.security.TestAuthentication.asRole;
import static com.afcgym.platform.configuration.security.TestAuthentication.asUser;
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
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PaymentAppointmentsControllerTest {

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
    void givenACashRenewal_whenReceptionReviewsAcceptsAndCancelsIt_thenTheMemberSeesEachStep() throws Exception {
        long userId = createAccount(createMember("Carla Efectivo"), "carla.efectivo");
        String started = mockMvc.perform(post("/v1/payments/me")
                        .with(asUser(Role.SOCIO, userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"membershipType\":\"MONTHLY\",\"method\":\"CASH\",\"appointmentAt\":\""
                                + tomorrowEvening + "\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long paymentId = ((Number) JsonPath.read(started, "$.id")).longValue();
        long appointmentId = ((Number) JsonPath.read(started, "$.appointment.id")).longValue();

        mockMvc.perform(get("/v1/payment-appointments").with(asRole(Role.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(appointmentId))
                .andExpect(jsonPath("$[0].status").value("PENDING"))
                .andExpect(jsonPath("$[0].memberName").value("Carla Efectivo"))
                .andExpect(jsonPath("$[0].memberPhone").value("5512345678"))
                .andExpect(jsonPath("$[0].amount").value(500.0))
                .andExpect(jsonPath("$[0].paymentReference").value(String.format("PAG-%06d", paymentId)));

        mockMvc.perform(patch("/v1/payment-appointments/" + appointmentId + "/accept").with(asRole(Role.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"));
        mockMvc.perform(get("/v1/payments/me/" + paymentId).with(asUser(Role.SOCIO, userId)))
                .andExpect(jsonPath("$.status").value("APPOINTMENT_CONFIRMED"));

        LocalDateTime suggestion = tomorrowEvening.plusDays(1);
        mockMvc.perform(patch("/v1/payment-appointments/" + appointmentId + "/cancel")
                        .with(asRole(Role.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Recepción cerrada\",\"suggestedAt\":\"" + suggestion + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.cancellationReason").value("Recepción cerrada"));
        mockMvc.perform(get("/v1/payments/me/" + paymentId).with(asUser(Role.SOCIO, userId)))
                .andExpect(jsonPath("$.status").value("APPOINTMENT_CANCELLED"))
                .andExpect(jsonPath("$.appointment.suggestedAt").value(suggestion.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)));
    }

    @Test
    void givenASocioToken_whenReadTheAgenda_thenReturnForbidden() throws Exception {
        mockMvc.perform(get("/v1/payment-appointments").with(asRole(Role.SOCIO)))
                .andExpect(status().isForbidden());
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
