package com.afcgym.platform.service.dto;

public record GatewayNotification(String type, String dataId, String requestId, String signature) {}
