package com.afcgym.platform.service.impl;

import com.afcgym.platform.configuration.EmailProperties;
import com.afcgym.platform.service.IEmailService;
import com.afcgym.platform.service.dto.PasswordResetRequestedEvent;
import com.afcgym.platform.utils.SpanishDateFormatter;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Service
@RequiredArgsConstructor
public class SmtpEmailServiceImpl implements IEmailService {

    private static final String PASSWORD_RESET_SUBJECT = "Recupera tu contraseña de AFC Gym";

    private final JavaMailSender mailSender;
    private final EmailProperties properties;

    @Override
    @Async
    @TransactionalEventListener
    public void sendPasswordReset(PasswordResetRequestedEvent event) {
        if (!properties.isConfigured()) {
            log.warn("A password reset was requested but email is not configured; set MAIL_HOST and MAIL_FROM");
            return;
        }
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(properties.from());
        message.setTo(event.email());
        message.setSubject(PASSWORD_RESET_SUBJECT);
        message.setText(passwordResetText(event));
        try {
            mailSender.send(message);
        } catch (MailException exception) {
            log.warn("Could not send the password reset email", exception);
        }
    }

    private String passwordResetText(PasswordResetRequestedEvent event) {
        LocalDateTime expiresAt = LocalDateTime.ofInstant(event.expiresAt(), ZoneId.systemDefault());
        StringBuilder text = new StringBuilder()
                .append("Hola ").append(event.fullName()).append(":\n\n")
                .append("Recibimos una solicitud para cambiar la contraseña de tu cuenta de AFC Gym.\n\n");
        if (properties.hasPasswordResetUrl()) {
            text.append("Para elegir una nueva, abre este enlace:\n")
                    .append(passwordResetLink(event.token())).append("\n\n")
                    .append("Si la app te lo pide, copia este código:\n");
        } else {
            text.append("Para elegir una nueva, copia este código en la app:\n");
        }
        return text.append(event.token()).append("\n\n")
                .append("Solo sirve una vez y vence a las ").append(SpanishDateFormatter.time(expiresAt))
                .append(" del ").append(SpanishDateFormatter.date(expiresAt.toLocalDate())).append(".\n\n")
                .append("Si no fuiste tú, ignora este correo: tu contraseña no cambia.\n")
                .toString();
    }

    private String passwordResetLink(String token) {
        String url = properties.passwordResetUrl().strip();
        String separator = url.contains("?") ? "&" : "?";
        return url + separator + "token=" + URLEncoder.encode(token, StandardCharsets.UTF_8);
    }
}
