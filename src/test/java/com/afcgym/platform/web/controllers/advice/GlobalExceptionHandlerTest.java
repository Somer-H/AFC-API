package com.afcgym.platform.web.controllers.advice;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.afcgym.platform.configuration.audit.AuditRecorder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletResponse;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    @Mock
    private AuditRecorder auditRecorder;

    @Test
    void givenAnUnexpectedError_whenHandled_thenAnswerAGeneric500WithoutTechnicalDetails() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler(auditRecorder);
        MockHttpServletResponse response = new MockHttpServletResponse();

        ErrorResponse body = handler.handleAnythingElse(
                new IllegalStateException("jdbc:postgresql://db:5432 password=secret"), response);

        assertEquals(500, response.getStatus());
        assertEquals(500, body.status());
        assertEquals("Ocurrió un error inesperado. Intenta de nuevo más tarde", body.message());
    }
}
