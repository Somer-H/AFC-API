package com.afcgym.platform.configuration;

import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(PaymentProperties.class)
public class PaymentConfiguration {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(15);
    private static final String BEARER_PREFIX = "Bearer ";

    @Bean
    public RestClient mercadoPagoRestClient(PaymentProperties properties) {
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(CONNECT_TIMEOUT).build());
        requestFactory.setReadTimeout(READ_TIMEOUT);
        PaymentProperties.MercadoPago mercadoPago = properties.mercadoPago();
        RestClient.Builder builder = RestClient.builder()
                .requestFactory(new BufferingClientHttpRequestFactory(requestFactory))
                .baseUrl(mercadoPago.apiUrl())
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
        if (mercadoPago.isConfigured()) {
            builder.defaultHeader(HttpHeaders.AUTHORIZATION, BEARER_PREFIX + mercadoPago.accessToken());
        }
        return builder.build();
    }
}
