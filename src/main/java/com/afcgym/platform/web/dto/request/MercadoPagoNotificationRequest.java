package com.afcgym.platform.web.dto.request;

public record MercadoPagoNotificationRequest(String type, String action, MercadoPagoNotificationData data) {}
