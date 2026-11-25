package com.afcgym.platform.service.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record MercadoPagoPreferenceRequest(
        List<MercadoPagoPreferenceItem> items,
        @JsonProperty("external_reference") String externalReference,
        @JsonProperty("notification_url") String notificationUrl,
        @JsonProperty("back_urls") Map<String, String> backUrls,
        @JsonProperty("auto_return") String autoReturn,
        @JsonProperty("payment_methods") Map<String, Object> paymentMethods,
        Boolean expires,
        @JsonProperty("expiration_date_to") String expirationDateTo) {}
