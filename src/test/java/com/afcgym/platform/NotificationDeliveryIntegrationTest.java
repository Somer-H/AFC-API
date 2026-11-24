package com.afcgym.platform;

import static com.afcgym.platform.configuration.security.TestAuthentication.asRole;
import static com.afcgym.platform.configuration.security.TestAuthentication.asUser;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.afcgym.platform.persistance.entities.enums.Role;
import com.afcgym.platform.service.INotificationService;
import com.afcgym.platform.service.IPushDeliveryService;
import com.afcgym.platform.service.dto.PushDeliveryReport;
import com.afcgym.platform.service.dto.PushMessage;
import com.afcgym.platform.utils.SpanishDateFormatter;
import com.jayway.jsonpath.JsonPath;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = "spring.datasource.url=jdbc:h2:mem:notificationdelivery;MODE=PostgreSQL;"
        + "DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;DB_CLOSE_DELAY=-1")
class NotificationDeliveryIntegrationTest {

    private static final String PHONE_TOKEN = "ExponentPushToken[phone-1]";
    private static final String TABLET_TOKEN = "ExponentPushToken[tablet-2]";

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private INotificationService notificationService;

    @MockitoBean
    private IPushDeliveryService pushDeliveryService;

    private MockMvc mockMvc;

    private LocalDate today;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
        today = LocalDate.now();
        when(pushDeliveryService.deliver(anyList())).thenAnswer(invocation -> acceptEveryDevice(invocation.getArgument(0)));
    }

    @AfterEach
    void cleanUp() {
        jdbc.update("DELETE FROM expiry_reminder");
        jdbc.update("DELETE FROM push_token");
        jdbc.update("DELETE FROM payment");
        jdbc.update("DELETE FROM member");
        jdbc.update("DELETE FROM app_user WHERE role = 'SOCIO'");
    }

    @Test
    void givenAMemberWithARegisteredDevice_whenACardPaymentIsCommitted_thenConfirmTheRenewalOnThatDevice() throws Exception {
        long memberId = createMember("Ana Renueva");
        registerDevice(createAccount(memberId, "ana.renueva"), PHONE_TOKEN);

        mockMvc.perform(post("/v1/members/" + memberId + "/payments")
                        .with(asRole(Role.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"membershipType\":\"MONTHLY\",\"method\":\"CARD\",\"cardLastFour\":\"4242\"}"))
                .andExpect(status().isCreated());

        String expectedBody = "Tu membresía quedó renovada. Vence el "
                + SpanishDateFormatter.date(today.plusDays(60)) + ".";
        verify(pushDeliveryService).deliver(argThat((List<PushMessage> messages) -> messages.size() == 1
                && messages.getFirst().to().equals(PHONE_TOKEN)
                && messages.getFirst().body().equals(expectedBody)));
    }

    @Test
    void givenACashPaymentStillPending_whenTheAdminApprovesIt_thenConfirmTheRenewalOnlyAfterTheApproval() throws Exception {
        long memberId = createMember("Beto Efectivo");
        registerDevice(createAccount(memberId, "beto.efectivo"), PHONE_TOKEN);

        String payment = mockMvc.perform(post("/v1/members/" + memberId + "/payments")
                        .with(asRole(Role.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"membershipType\":\"MONTHLY\",\"method\":\"CASH\",\"scheduledDate\":\""
                                + today.plusDays(1) + "\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        verify(pushDeliveryService, never()).deliver(anyList());

        mockMvc.perform(patch("/v1/payments/" + readId(payment) + "/approve").with(asRole(Role.ADMIN)))
                .andExpect(status().isOk());

        verify(pushDeliveryService, times(1)).deliver(anyList());
    }

    @Test
    void givenMembershipsInsideAndOutsideTheWindow_whenTheJobRunsTwice_thenRemindOnceAndOnlyWhoHasNotRenewed() {
        long expiringSoon = insertMember("SOC-NOTIF-1", today.plusDays(2));
        long alreadyRenewed = insertMember("SOC-NOTIF-2", today.plusDays(32));
        insertMember("SOC-NOTIF-3", today.plusDays(1));
        insertDevice(expiringSoon, PHONE_TOKEN);
        insertDevice(alreadyRenewed, TABLET_TOKEN);

        int firstRun = notificationService.sendExpiryReminders(today, 3);
        int secondRun = notificationService.sendExpiryReminders(today, 3);

        verify(pushDeliveryService, times(1)).deliver(argThat((List<PushMessage> messages) -> messages.size() == 1
                && messages.getFirst().to().equals(PHONE_TOKEN)));
        assertEquals(1, firstRun);
        assertEquals(0, secondRun);
        Integer reminders = jdbc.queryForObject(
                "SELECT COUNT(*) FROM expiry_reminder WHERE member_id = ? AND due_date = ?",
                Integer.class, expiringSoon, today.plusDays(2));
        assertEquals(1, reminders);
    }

    private PushDeliveryReport acceptEveryDevice(List<PushMessage> messages) {
        return new PushDeliveryReport(messages.stream().map(PushMessage::to).collect(Collectors.toSet()), Set.of());
    }

    private void registerDevice(long userId, String token) throws Exception {
        mockMvc.perform(post("/v1/push-tokens")
                        .with(asUser(Role.SOCIO, userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"" + token + "\"}"))
                .andExpect(status().isCreated());
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

    private long insertMember(String code, LocalDate dueDate) {
        jdbc.update("INSERT INTO member (code, full_name, phone, membership_type, due_date, active) "
                + "VALUES (?, ?, '5512345678', 'MONTHLY', ?, TRUE)", code, "Socio " + code, dueDate);
        return jdbc.queryForObject("SELECT id FROM member WHERE code = ?", Long.class, code);
    }

    private void insertDevice(long memberId, String token) {
        jdbc.update("INSERT INTO push_token (member_id, token) VALUES (?, ?)", memberId, token);
    }

    private long readId(String response) {
        return ((Number) JsonPath.read(response, "$.id")).longValue();
    }
}
