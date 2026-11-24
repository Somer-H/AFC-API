package com.afcgym.platform.service.dto;

import java.util.Map;

public record ExpoPushTicket(String status, String message, Map<String, Object> details) {

    private static final String OK_STATUS = "ok";
    private static final String ERROR_DETAIL = "error";
    private static final String DEVICE_NOT_REGISTERED = "DeviceNotRegistered";

    public boolean accepted() {
        return OK_STATUS.equals(status);
    }

    public boolean deviceNotRegistered() {
        return details != null && DEVICE_NOT_REGISTERED.equals(details.get(ERROR_DETAIL));
    }
}
