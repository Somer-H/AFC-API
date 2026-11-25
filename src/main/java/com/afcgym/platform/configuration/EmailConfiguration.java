package com.afcgym.platform.configuration;

import java.nio.charset.StandardCharsets;
import java.util.Properties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.scheduling.annotation.EnableAsync;

@Configuration
@EnableAsync
@EnableConfigurationProperties(EmailProperties.class)
public class EmailConfiguration {

    private static final int IMPLICIT_TLS_PORT = 465;
    private static final String TIMEOUT_MILLISECONDS = "10000";

    @Bean
    public JavaMailSenderImpl emailSender(EmailProperties properties) {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setDefaultEncoding(StandardCharsets.UTF_8.name());
        Properties smtp = sender.getJavaMailProperties();
        smtp.put("mail.smtp.connectiontimeout", TIMEOUT_MILLISECONDS);
        smtp.put("mail.smtp.timeout", TIMEOUT_MILLISECONDS);
        smtp.put("mail.smtp.writetimeout", TIMEOUT_MILLISECONDS);
        if (!properties.isConfigured()) {
            return sender;
        }
        sender.setHost(properties.host());
        sender.setPort(properties.port());
        if (properties.hasCredentials()) {
            sender.setUsername(properties.username());
            sender.setPassword(properties.password());
            smtp.put("mail.smtp.auth", "true");
        }
        if (properties.port() == IMPLICIT_TLS_PORT) {
            smtp.put("mail.smtp.ssl.enable", "true");
        } else {
            smtp.put("mail.smtp.starttls.enable", "true");
        }
        return sender;
    }
}
