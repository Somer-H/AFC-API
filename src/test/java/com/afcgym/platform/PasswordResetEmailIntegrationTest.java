package com.afcgym.platform;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:passwordresetemail;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;"
                + "DEFAULT_NULL_ORDERING=HIGH;DB_CLOSE_DELAY=-1",
        "app.email.host=smtp.afcgym.test",
        "app.email.from=no-responder@afcgym.test",
        "app.email.password-reset-url=https://app.afcgym.test/recuperar"})
class PasswordResetEmailIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private JdbcTemplate jdbc;

    @MockitoBean
    private JavaMailSender mailSender;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
    }

    @Test
    void givenAKnownEmail_whenRequestPasswordReset_thenEmailTheSavedCodeAfterCommitting() throws Exception {
        mockMvc.perform(post("/v1/auth/password-resets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"admin@afcgym.local\"}"))
                .andExpect(status().isNoContent());

        ArgumentCaptor<SimpleMailMessage> message = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender, timeout(5000)).send(message.capture());
        String token = jdbc.queryForObject("SELECT t.token FROM password_reset_token t JOIN app_user u "
                + "ON u.id = t.user_id WHERE u.email = 'admin@afcgym.local' ORDER BY t.id DESC LIMIT 1", String.class);
        assertArrayEquals(new String[] {"admin@afcgym.local"}, message.getValue().getTo());
        assertTrue(message.getValue().getText().contains("https://app.afcgym.test/recuperar?token=" + token));
    }

    @Test
    void givenAnUnknownEmail_whenRequestPasswordReset_thenSendNoEmail() throws Exception {
        mockMvc.perform(post("/v1/auth/password-resets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"nadie@afcgym.local\"}"))
                .andExpect(status().isNoContent());

        verify(mailSender, after(1000).never()).send(any(SimpleMailMessage.class));
    }
}
