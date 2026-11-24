package com.afcgym.platform.web.controllers;

import static com.afcgym.platform.configuration.security.TestAuthentication.asRole;
import static com.afcgym.platform.configuration.security.TestAuthentication.asSession;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
class AnnouncementsControllerTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

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
    void givenASocioToken_whenPublish_thenReturnForbidden() throws Exception {
        mockMvc.perform(post("/v1/announcements")
                        .with(asRole(Role.SOCIO))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("Aviso", "INFO", today, null)))
                .andExpect(status().isForbidden());
    }

    @Test
    void givenAValidAnnouncement_whenPublish_thenReturnItAsCurrent() throws Exception {
        mockMvc.perform(post("/v1/announcements")
                        .with(asRole(Role.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("Clases suspendidas", "URGENT", today, today.plusDays(1))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Clases suspendidas"))
                .andExpect(jsonPath("$.type").value("URGENT"))
                .andExpect(jsonPath("$.startDate").value(today.toString()))
                .andExpect(jsonPath("$.current").value(true));
    }

    @Test
    void givenNoMessage_whenPublish_thenReturnBadRequest() throws Exception {
        mockMvc.perform(post("/v1/announcements")
                        .with(asRole(Role.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Aviso\",\"type\":\"INFO\",\"startDate\":\"" + today + "\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void givenNoDate_whenPublish_thenReturnBadRequest() throws Exception {
        mockMvc.perform(post("/v1/announcements")
                        .with(asRole(Role.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Aviso\",\"message\":\"Mensaje\",\"type\":\"INFO\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void givenAnUnknownType_whenPublish_thenReturnBadRequest() throws Exception {
        mockMvc.perform(post("/v1/announcements")
                        .with(asRole(Role.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("Aviso", "ALERT", today, null)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void givenPastCurrentAndScheduledAnnouncements_whenASocioListsActive_thenSeeOnlyTheCurrentOnes() throws Exception {
        publish("Vencido", "INFO", today.minusDays(5), today);
        mockMvc.perform(post("/v1/announcements")
                        .with(asRole(Role.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("Programado", "INFO", today.plusDays(3), null)))
                .andExpect(status().isCreated());
        publish("Promo", "PROMOTION", today, null);
        publish("Urgente", "URGENT", today.minusDays(1), null);

        mockMvc.perform(get("/v1/announcements/active").with(asRole(Role.SOCIO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].title").value("Urgente"))
                .andExpect(jsonPath("$[1].title").value("Vencido"))
                .andExpect(jsonPath("$[2].title").value("Promo"));

        mockMvc.perform(get("/v1/announcements").with(asRole(Role.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(4))
                .andExpect(jsonPath("$[0].title").value("Programado"))
                .andExpect(jsonPath("$[0].current").value(false));
    }

    @Test
    void givenASocioToken_whenListEveryAnnouncement_thenReturnForbidden() throws Exception {
        mockMvc.perform(get("/v1/announcements").with(asRole(Role.SOCIO)))
                .andExpect(status().isForbidden());
    }

    @Test
    void givenADismissedAnnouncement_whenListActive_thenItOnlyDisappearsForThatSession() throws Exception {
        long announcementId = publish("Aviso", "INFO", today, null);

        mockMvc.perform(post("/v1/announcements/" + announcementId + "/dismiss")
                        .with(asSession(Role.SOCIO, "session-a")))
                .andExpect(status().isNoContent());
        mockMvc.perform(post("/v1/announcements/" + announcementId + "/dismiss")
                        .with(asSession(Role.SOCIO, "session-a")))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/v1/announcements/active").with(asSession(Role.SOCIO, "session-a")))
                .andExpect(jsonPath("$.length()").value(0));
        mockMvc.perform(get("/v1/announcements/active").with(asSession(Role.SOCIO, "session-b")))
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void givenAnUnknownAnnouncement_whenDismiss_thenReturnNotFound() throws Exception {
        mockMvc.perform(post("/v1/announcements/999999/dismiss").with(asRole(Role.SOCIO)))
                .andExpect(status().isNotFound());
    }

    @Test
    void givenASocioToken_whenDelete_thenReturnForbidden() throws Exception {
        long announcementId = publish("Aviso", "INFO", today, null);

        mockMvc.perform(delete("/v1/announcements/" + announcementId).with(asRole(Role.SOCIO)))
                .andExpect(status().isForbidden());
    }

    @Test
    void givenAnAnnouncement_whenAdminDeletesIt_thenSociosStopSeeingIt() throws Exception {
        long announcementId = publish("Aviso", "INFO", today, null);

        mockMvc.perform(delete("/v1/announcements/" + announcementId).with(asRole(Role.ADMIN)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/v1/announcements/active").with(asRole(Role.SOCIO)))
                .andExpect(jsonPath("$.length()").value(0));
        mockMvc.perform(delete("/v1/announcements/" + announcementId).with(asRole(Role.ADMIN)))
                .andExpect(status().isNotFound());
    }

    private long publish(String title, String type, LocalDate startDate, LocalDate endDate) throws Exception {
        String response = mockMvc.perform(post("/v1/announcements")
                        .with(asRole(Role.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(title, type, startDate, endDate)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(response, "$.id")).longValue();
    }

    private String body(String title, String type, LocalDate startDate, LocalDate endDate) {
        return "{\"title\":\"" + title + "\",\"message\":\"Mensaje del aviso\",\"type\":\"" + type + "\","
                + "\"startDate\":\"" + startDate + "\""
                + (endDate != null ? ",\"endDate\":\"" + endDate + "\"" : "") + "}";
    }
}
