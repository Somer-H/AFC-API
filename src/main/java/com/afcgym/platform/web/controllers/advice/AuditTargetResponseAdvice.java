package com.afcgym.platform.web.controllers.advice;

import com.afcgym.platform.configuration.audit.AuditRecorder;
import java.lang.reflect.RecordComponent;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

@RestControllerAdvice
public class AuditTargetResponseAdvice implements ResponseBodyAdvice<Object> {

    private static final String ID_COMPONENT = "id";

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request, ServerHttpResponse response) {
        if (body == null || !body.getClass().isRecord() || !(request instanceof ServletServerHttpRequest servletRequest)) {
            return body;
        }
        for (RecordComponent component : body.getClass().getRecordComponents()) {
            if (!ID_COMPONENT.equals(component.getName())) {
                continue;
            }
            try {
                Object id = component.getAccessor().invoke(body);
                if (id != null) {
                    servletRequest.getServletRequest().setAttribute(AuditRecorder.TARGET_ID_ATTRIBUTE, id.toString());
                }
            } catch (ReflectiveOperationException exception) {
                return body;
            }
        }
        return body;
    }
}
