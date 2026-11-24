package com.afcgym.platform.web.controllers.advice;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(int status, String message, Map<String, String> fields) {

    public ErrorResponse(int status, String message) {
        this(status, message, null);
    }
}
