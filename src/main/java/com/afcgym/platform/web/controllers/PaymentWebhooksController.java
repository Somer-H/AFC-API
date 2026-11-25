package com.afcgym.platform.web.controllers;

import com.afcgym.platform.service.IPaymentService;
import com.afcgym.platform.service.dto.GatewayNotification;
import com.afcgym.platform.web.dto.request.MercadoPagoNotificationRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/payment-webhooks")
@RequiredArgsConstructor
@Tag(name = "Payment webhooks")
public class PaymentWebhooksController {

    private final IPaymentService paymentService;

    @PostMapping("/mercado-pago")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Mercado Pago payment notifications, verified with the webhook secret signature")
    public void mercadoPago(
            @RequestParam(name = "type", required = false) String type,
            @RequestParam(name = "data.id", required = false) String dataId,
            @RequestHeader(name = "x-signature", required = false) String signature,
            @RequestHeader(name = "x-request-id", required = false) String requestId,
            @RequestBody(required = false) MercadoPagoNotificationRequest body) {
        paymentService.handleGatewayNotification(new GatewayNotification(
                type != null ? type : typeOf(body),
                dataId != null ? dataId : dataIdOf(body),
                requestId,
                signature));
    }

    private String typeOf(MercadoPagoNotificationRequest body) {
        return body == null ? null : body.type();
    }

    private String dataIdOf(MercadoPagoNotificationRequest body) {
        return body == null || body.data() == null ? null : body.data().id();
    }
}
