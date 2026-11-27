package com.afcgym.platform.service.impl;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.afcgym.platform.configuration.EmailProperties;
import com.afcgym.platform.service.dto.PasswordResetRequestedEvent;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

@ExtendWith(MockitoExtension.class)
class SmtpEmailServiceImplTest {

    private static final Instant EXPIRES_AT =
            LocalDateTime.of(2026, 11, 25, 18, 30).atZone(ZoneId.systemDefault()).toInstant();
    private static final PasswordResetRequestedEvent EVENT =
            new PasswordResetRequestedEvent("ana@correo.com", "Ana López", "a1b2c3d4", EXPIRES_AT);

    @Mock
    private JavaMailSender mailSender;

    @Captor
    private ArgumentCaptor<SimpleMailMessage> messageCaptor;

    @Test
    void givenALinkUrl_whenSendPasswordReset_thenEmailTheLinkAndTheCode() {
        service("https://app.afcgym.mx/recuperar").sendPasswordReset(EVENT);

        verify(mailSender).send(messageCaptor.capture());
        SimpleMailMessage message = messageCaptor.getValue();
        assertEquals("no-responder@afcgym.mx", message.getFrom());
        assertArrayEquals(new String[] {"ana@correo.com"}, message.getTo());
        assertEquals("Recupera tu contraseña de AFC Gym", message.getSubject());
        String text = message.getText();
        assertTrue(text.startsWith("Hola Ana López:"));
        assertTrue(text.contains("https://app.afcgym.mx/recuperar?token=a1b2c3d4"));
        assertTrue(text.contains("Si la app te lo pide, copia este código:\na1b2c3d4"));
        assertTrue(text.contains("vence a las 18:30 del 25/11/2026"));
    }

    @Test
    void givenALinkUrlWithQuery_whenSendPasswordReset_thenAppendTheTokenAsAnotherParameter() {
        service("afcapp://recuperar?origen=correo").sendPasswordReset(EVENT);

        verify(mailSender).send(messageCaptor.capture());
        assertTrue(messageCaptor.getValue().getText().contains("afcapp://recuperar?origen=correo&token=a1b2c3d4"));
    }

    @Test
    void givenNoLinkUrl_whenSendPasswordReset_thenEmailOnlyTheCode() {
        service("").sendPasswordReset(EVENT);

        verify(mailSender).send(messageCaptor.capture());
        String text = messageCaptor.getValue().getText();
        assertTrue(text.contains("Para elegir una nueva, copia este código en la app:\na1b2c3d4"));
        assertFalse(text.contains("enlace"));
    }

    @Test
    void givenNoMailServer_whenSendPasswordReset_thenSendNothing() {
        new SmtpEmailServiceImpl(mailSender, new EmailProperties("", 587, "", "", "", ""))
                .sendPasswordReset(EVENT);

        verifyNoInteractions(mailSender);
    }

    @Test
    void givenTheMailServerFails_whenSendPasswordReset_thenDoNotThrow() {
        doThrow(new MailSendException("SMTP caído")).when(mailSender).send(any(SimpleMailMessage.class));

        assertDoesNotThrow(() -> service("").sendPasswordReset(EVENT));
    }

    private SmtpEmailServiceImpl service(String passwordResetUrl) {
        return new SmtpEmailServiceImpl(mailSender, new EmailProperties(
                "smtp.afcgym.mx", 587, "usuario", "clave", "no-responder@afcgym.mx", passwordResetUrl));
    }
}
