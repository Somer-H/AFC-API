package com.afcgym.platform.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.afcgym.platform.configuration.PaymentProperties;
import com.afcgym.platform.exception.BadRequestException;
import com.afcgym.platform.exception.ServiceUnavailableException;
import com.afcgym.platform.persistance.entities.PaymentEntity;
import com.afcgym.platform.persistance.entities.enums.MembershipType;
import com.afcgym.platform.persistance.entities.enums.PaymentMethod;
import com.afcgym.platform.persistance.entities.enums.PaymentStatus;
import com.afcgym.platform.service.dto.GatewayCheckout;
import com.afcgym.platform.service.dto.GatewayPayment;
import com.afcgym.platform.utils.HmacSha256;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class MercadoPagoGatewayServiceImplTest {

    private static final String API_URL = "https://mp.test";
    private static final String WEBHOOK_SECRET = "webhook-secret";
    private static final String PREFERENCE_RESPONSE = "{\"id\":\"pref-7\","
            + "\"init_point\":\"https://www.mercadopago.com.mx/checkout/v1/redirect?pref_id=pref-7\","
            + "\"sandbox_init_point\":\"https://sandbox.mercadopago.com.mx/checkout/v1/redirect?pref_id=pref-7\"}";

    private MockRestServiceServer server;

    @Test
    void givenTestCredentials_whenCreateCheckout_thenSendThePreferenceAndReturnTheSandboxUrl() {
        MercadoPagoGatewayServiceImpl gateway = gatewayWith("TEST-123");
        server.expect(requestTo(API_URL + "/checkout/preferences"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.items[0].unit_price").value(500.0))
                .andExpect(jsonPath("$.items[0].currency_id").value("MXN"))
                .andExpect(jsonPath("$.items[0].title").value("Renovación de membresía mensual AFC Gym"))
                .andExpect(jsonPath("$.external_reference").value("7"))
                .andExpect(jsonPath("$.notification_url").value("https://api.afc.test/v1/payment-webhooks/mercado-pago"))
                .andExpect(jsonPath("$.payment_methods.installments").value(1))
                .andExpect(jsonPath("$.payment_methods.excluded_payment_types[0].id").value("ticket"))
                .andExpect(jsonPath("$.back_urls").doesNotExist())
                .andExpect(jsonPath("$.expires").value(true))
                .andRespond(withSuccess(PREFERENCE_RESPONSE, MediaType.APPLICATION_JSON));

        GatewayCheckout checkout = gateway.createCheckout(cardPayment());

        server.verify();
        assertEquals("pref-7", checkout.preferenceId());
        assertEquals("https://sandbox.mercadopago.com.mx/checkout/v1/redirect?pref_id=pref-7", checkout.checkoutUrl());
    }

    @Test
    void givenProductionCredentials_whenCreateCheckout_thenReturnTheRegularCheckoutUrl() {
        MercadoPagoGatewayServiceImpl gateway = gatewayWith("APP_USR-123");
        server.expect(requestTo(API_URL + "/checkout/preferences"))
                .andRespond(withSuccess(PREFERENCE_RESPONSE, MediaType.APPLICATION_JSON));

        GatewayCheckout checkout = gateway.createCheckout(cardPayment());

        assertEquals("https://www.mercadopago.com.mx/checkout/v1/redirect?pref_id=pref-7", checkout.checkoutUrl());
    }

    @Test
    void givenMercadoPagoIsDown_whenCreateCheckout_thenThrowServiceUnavailableException() {
        MercadoPagoGatewayServiceImpl gateway = gatewayWith("TEST-123");
        server.expect(requestTo(API_URL + "/checkout/preferences")).andRespond(withServerError());

        ServiceUnavailableException exception = assertThrows(ServiceUnavailableException.class,
                () -> gateway.createCheckout(cardPayment()));

        assertEquals("No pudimos iniciar el pago con tarjeta. Intenta de nuevo en unos minutos", exception.getMessage());
    }

    @Test
    void givenASignatureMadeWithTheWebhookSecret_whenIsSignatureValid_thenAcceptIt() {
        MercadoPagoGatewayServiceImpl gateway = gatewayWith("TEST-123");
        String hash = HmacSha256.hex(WEBHOOK_SECRET, "id:123456;request-id:request-1;ts:1700000000;");

        assertTrue(gateway.isSignatureValid("123456", "request-1", "ts=1700000000,v1=" + hash));
    }

    @Test
    void givenASignatureForAnotherPayment_whenIsSignatureValid_thenRejectIt() {
        MercadoPagoGatewayServiceImpl gateway = gatewayWith("TEST-123");
        String hash = HmacSha256.hex(WEBHOOK_SECRET, "id:999999;request-id:request-1;ts:1700000000;");

        assertFalse(gateway.isSignatureValid("123456", "request-1", "ts=1700000000,v1=" + hash));
        assertFalse(gateway.isSignatureValid("123456", "request-1", "v1=" + hash));
        assertFalse(gateway.isSignatureValid("123456", null, "ts=1700000000,v1=" + hash));
    }

    @Test
    void givenNoWebhookSecretConfigured_whenIsSignatureValid_thenRejectEverything() {
        RestClient.Builder builder = RestClient.builder().baseUrl(API_URL);
        MercadoPagoGatewayServiceImpl gateway = new MercadoPagoGatewayServiceImpl(builder.build(),
                properties("TEST-123", null));
        String hash = HmacSha256.hex(WEBHOOK_SECRET, "id:123456;request-id:request-1;ts:1700000000;");

        assertFalse(gateway.isSignatureValid("123456", "request-1", "ts=1700000000,v1=" + hash));
    }

    @Test
    void givenAPaymentId_whenFetchPayment_thenReadItsStatusAmountAndReference() {
        MercadoPagoGatewayServiceImpl gateway = gatewayWith("TEST-123");
        server.expect(requestTo(API_URL + "/v1/payments/123456"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"id\":123456,\"status\":\"approved\",\"status_detail\":\"accredited\","
                        + "\"external_reference\":\"7\",\"transaction_amount\":500.0,\"currency_id\":\"MXN\","
                        + "\"payer\":{\"email\":\"socio@test.com\"}}", MediaType.APPLICATION_JSON));

        GatewayPayment payment = gateway.fetchPayment("123456");

        server.verify();
        assertEquals("123456", payment.id());
        assertEquals("approved", payment.status());
        assertEquals("7", payment.externalReference());
        assertEquals(0, new BigDecimal("500.00").compareTo(payment.amount()));
        assertEquals("MXN", payment.currency());
    }

    @Test
    void givenAnIdThatIsNotANumber_whenFetchPayment_thenThrowBadRequestWithoutCallingMercadoPago() {
        MercadoPagoGatewayServiceImpl gateway = gatewayWith("TEST-123");

        assertThrows(BadRequestException.class, () -> gateway.fetchPayment("../users/me"));

        server.verify();
    }

    @Test
    void givenMercadoPagoIsDown_whenFetchPayment_thenThrowServiceUnavailableSoTheNotificationIsRetried() {
        MercadoPagoGatewayServiceImpl gateway = gatewayWith("TEST-123");
        server.expect(requestTo(API_URL + "/v1/payments/123456")).andRespond(withServerError());

        assertThrows(ServiceUnavailableException.class, () -> gateway.fetchPayment("123456"));
    }

    private MercadoPagoGatewayServiceImpl gatewayWith(String accessToken) {
        RestClient.Builder builder = RestClient.builder().baseUrl(API_URL);
        server = MockRestServiceServer.bindTo(builder).build();
        return new MercadoPagoGatewayServiceImpl(builder.build(), properties(accessToken, WEBHOOK_SECRET));
    }

    private PaymentProperties properties(String accessToken, String webhookSecret) {
        return new PaymentProperties(30, 30, new PaymentProperties.BankTransfer(null, null, null),
                new PaymentProperties.MercadoPago(API_URL, accessToken, webhookSecret,
                        "https://api.afc.test/v1/payment-webhooks/mercado-pago", null));
    }

    private PaymentEntity cardPayment() {
        return PaymentEntity.builder()
                .id(7L)
                .memberId(1L)
                .membershipType(MembershipType.MONTHLY)
                .method(PaymentMethod.CARD)
                .status(PaymentStatus.AWAITING_GATEWAY)
                .amount(new BigDecimal("500.00"))
                .createdAt(Instant.now())
                .build();
    }
}
