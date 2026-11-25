package com.afcgym.platform.service.impl;

import com.afcgym.platform.configuration.PaymentProperties;
import com.afcgym.platform.exception.BadRequestException;
import com.afcgym.platform.exception.ServiceUnavailableException;
import com.afcgym.platform.persistance.entities.PaymentEntity;
import com.afcgym.platform.service.IPaymentGatewayService;
import com.afcgym.platform.service.dto.GatewayCheckout;
import com.afcgym.platform.service.dto.GatewayPayment;
import com.afcgym.platform.service.dto.MercadoPagoPaymentResponse;
import com.afcgym.platform.service.dto.MercadoPagoPreferenceItem;
import com.afcgym.platform.service.dto.MercadoPagoPreferenceRequest;
import com.afcgym.platform.service.dto.MercadoPagoPreferenceResponse;
import com.afcgym.platform.utils.HmacSha256;
import com.afcgym.platform.utils.PaymentReferenceFormatter;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Slf4j
@Service
@RequiredArgsConstructor
public class MercadoPagoGatewayServiceImpl implements IPaymentGatewayService {

    private static final String PREFERENCES_PATH = "/checkout/preferences";
    private static final String PAYMENT_PATH = "/v1/payments/{id}";
    private static final String CURRENCY = "MXN";
    private static final String AUTO_RETURN_APPROVED = "approved";
    private static final Duration CHECKOUT_LIFETIME = Duration.ofHours(24);
    private static final DateTimeFormatter EXPIRATION_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
    private static final Pattern GATEWAY_PAYMENT_ID = Pattern.compile("^[0-9]{1,20}$");
    private static final String SIGNATURE_TIMESTAMP = "ts";
    private static final String SIGNATURE_HASH = "v1";

    @Qualifier("mercadoPagoRestClient")
    private final RestClient mercadoPagoRestClient;
    private final PaymentProperties paymentProperties;

    @Override
    public boolean isConfigured() {
        return paymentProperties.mercadoPago().isConfigured();
    }

    @Override
    public GatewayCheckout createCheckout(PaymentEntity payment) {
        PaymentProperties.MercadoPago mercadoPago = paymentProperties.mercadoPago();
        String title = "Renovación de membresía "
                + PaymentReferenceFormatter.membershipName(payment.getMembershipType()) + " AFC Gym";
        Map<String, String> backUrls = hasText(mercadoPago.backUrl())
                ? Map.of("success", mercadoPago.backUrl(), "failure", mercadoPago.backUrl(), "pending", mercadoPago.backUrl())
                : null;
        MercadoPagoPreferenceRequest request = new MercadoPagoPreferenceRequest(
                List.of(new MercadoPagoPreferenceItem(
                        payment.getMembershipType().name(), title, 1, CURRENCY, payment.getAmount())),
                String.valueOf(payment.getId()),
                hasText(mercadoPago.notificationUrl()) ? mercadoPago.notificationUrl() : null,
                backUrls,
                backUrls == null ? null : AUTO_RETURN_APPROVED,
                Map.of("excluded_payment_types", List.of(Map.of("id", "ticket"), Map.of("id", "atm")),
                        "installments", 1),
                true,
                ZonedDateTime.now().plus(CHECKOUT_LIFETIME).format(EXPIRATION_FORMAT));
        MercadoPagoPreferenceResponse response;
        try {
            response = mercadoPagoRestClient.post()
                    .uri(PREFERENCES_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(MercadoPagoPreferenceResponse.class);
        } catch (RestClientException exception) {
            log.warn("Mercado Pago preference request failed: {}", exception.getClass().getSimpleName());
            throw checkoutUnavailable();
        }
        if (response == null || response.id() == null) {
            throw checkoutUnavailable();
        }
        String checkoutUrl = mercadoPago.usesTestCredentials() && response.sandboxInitPoint() != null
                ? response.sandboxInitPoint()
                : response.initPoint();
        if (checkoutUrl == null) {
            throw checkoutUnavailable();
        }
        return new GatewayCheckout(response.id(), checkoutUrl);
    }

    @Override
    public boolean isSignatureValid(String dataId, String requestId, String signature) {
        if (!isConfigured() || dataId == null || requestId == null || signature == null) {
            return false;
        }
        String timestamp = null;
        String hash = null;
        for (String part : signature.split(",")) {
            String[] keyValue = part.trim().split("=", 2);
            if (keyValue.length != 2) {
                continue;
            }
            if (SIGNATURE_TIMESTAMP.equals(keyValue[0])) {
                timestamp = keyValue[1];
            } else if (SIGNATURE_HASH.equals(keyValue[0])) {
                hash = keyValue[1];
            }
        }
        if (timestamp == null || hash == null) {
            return false;
        }
        String manifest = "id:" + dataId.toLowerCase(Locale.ROOT) + ";request-id:" + requestId + ";ts:" + timestamp + ";";
        return HmacSha256.matches(paymentProperties.mercadoPago().webhookSecret(), manifest, hash);
    }

    @Override
    public GatewayPayment fetchPayment(String gatewayPaymentId) {
        if (gatewayPaymentId == null || !GATEWAY_PAYMENT_ID.matcher(gatewayPaymentId).matches()) {
            throw new BadRequestException("El identificador del pago no es válido");
        }
        MercadoPagoPaymentResponse response;
        try {
            response = mercadoPagoRestClient.get()
                    .uri(PAYMENT_PATH, gatewayPaymentId)
                    .retrieve()
                    .body(MercadoPagoPaymentResponse.class);
        } catch (RestClientException exception) {
            log.warn("Mercado Pago payment lookup failed: {}", exception.getClass().getSimpleName());
            throw new ServiceUnavailableException("No pudimos consultar el pago en Mercado Pago");
        }
        if (response == null || response.id() == null) {
            throw new ServiceUnavailableException("No pudimos consultar el pago en Mercado Pago");
        }
        return new GatewayPayment(String.valueOf(response.id()), response.status(), response.statusDetail(),
                response.externalReference(), response.transactionAmount(), response.currencyId());
    }

    private ServiceUnavailableException checkoutUnavailable() {
        return new ServiceUnavailableException("No pudimos iniciar el pago con tarjeta. Intenta de nuevo en unos minutos");
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
