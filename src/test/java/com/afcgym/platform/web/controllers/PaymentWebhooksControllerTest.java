package com.afcgym.platform.web.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.afcgym.platform.service.IPaymentGatewayService;
import com.afcgym.platform.service.dto.GatewayPayment;
import com.afcgym.platform.utils.HmacSha256;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PaymentWebhooksControllerTest {

    private static final String WEBHOOK_SECRET = "webhook-secret-for-tests";
    private static final String GATEWAY_PAYMENT_ID = "123456";
    private static final String REQUEST_ID = "request-1";
    private static final String TIMESTAMP = "1700000000";

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private JdbcTemplate jdbc;

    @MockitoSpyBean
    private IPaymentGatewayService paymentGatewayService;

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
    void givenASignedApprovedNotification_whenItArrivesTwice_thenApproveOnceAndRenewOnce() throws Exception {
        long memberId = insertMember("SOC-HOOK-1", today.plusDays(5));
        long paymentId = insertOnlineCardPayment(memberId);
        doReturn(new GatewayPayment(GATEWAY_PAYMENT_ID, "approved", "accredited", String.valueOf(paymentId),
                new BigDecimal("500.00"), "MXN")).when(paymentGatewayService).fetchPayment(GATEWAY_PAYMENT_ID);
        String signature = "ts=" + TIMESTAMP + ",v1=" + HmacSha256.hex(WEBHOOK_SECRET,
                "id:" + GATEWAY_PAYMENT_ID + ";request-id:" + REQUEST_ID + ";ts:" + TIMESTAMP + ";");

        notify("payment", signature).andExpect(status().isOk());
        notify("payment", signature).andExpect(status().isOk());

        assertEquals("APPROVED", jdbc.queryForObject("SELECT status FROM payment WHERE id = ?", String.class, paymentId));
        assertEquals(GATEWAY_PAYMENT_ID, jdbc.queryForObject(
                "SELECT gateway_payment_id FROM payment WHERE id = ?", String.class, paymentId));
        assertEquals(today.plusDays(35), jdbc.queryForObject(
                "SELECT due_date FROM member WHERE id = ?", LocalDate.class, memberId));
    }

    @Test
    void givenAForgedSignature_whenTheNotificationArrives_thenReturnUnauthorizedWithoutAskingMercadoPago() throws Exception {
        notify("payment", "ts=" + TIMESTAMP + ",v1=deadbeef").andExpect(status().isUnauthorized());

        verify(paymentGatewayService, never()).fetchPayment(any());
    }

    @Test
    void givenAMerchantOrderNotification_whenItArrives_thenAnswerOkAndIgnoreIt() throws Exception {
        notify("merchant_order", null).andExpect(status().isOk());

        verify(paymentGatewayService, never()).fetchPayment(any());
    }

    private ResultActions notify(String type, String signature) throws Exception {
        MockHttpServletRequestBuilder request = post("/v1/payment-webhooks/mercado-pago")
                .param("type", type)
                .param("data.id", GATEWAY_PAYMENT_ID)
                .header("x-request-id", REQUEST_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"type\":\"" + type + "\",\"action\":\"payment.updated\",\"data\":{\"id\":\""
                        + GATEWAY_PAYMENT_ID + "\"}}");
        if (signature != null) {
            request.header("x-signature", signature);
        }
        return mockMvc.perform(request);
    }

    private long insertMember(String code, LocalDate dueDate) {
        jdbc.update("INSERT INTO member (code, full_name, phone, membership_type, due_date, active) "
                + "VALUES (?, 'Socio Webhook', '5512345678', 'MONTHLY', ?, TRUE)", code, dueDate);
        return jdbc.queryForObject("SELECT id FROM member WHERE code = ?", Long.class, code);
    }

    private long insertOnlineCardPayment(long memberId) {
        jdbc.update("INSERT INTO payment (member_id, membership_type, method, status, amount, discount_percent, "
                + "gateway_preference_id) VALUES (?, 'MONTHLY', 'CARD', 'AWAITING_GATEWAY', 500.00, 0, 'pref-test')",
                memberId);
        return jdbc.queryForObject("SELECT MAX(id) FROM payment WHERE member_id = ?", Long.class, memberId);
    }
}
