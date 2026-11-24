package com.afcgym.platform.configuration.security;

import com.afcgym.platform.configuration.security.jwt.RevokedTokenValidator;
import com.afcgym.platform.persistance.entities.enums.Role;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableConfigurationProperties(SecurityProperties.class)
@RequiredArgsConstructor
public class SecurityConfiguration {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final int MINIMUM_SECRET_BYTES = 32;
    private static final String ROLE_CLAIM = "role";
    private static final String ROLE_AUTHORITY_PREFIX = "ROLE_";

    private static final String[] PUBLIC_PATHS = {
            "/actuator/health",
            "/actuator/health/**",
            "/swagger-ui.html",
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/h2-console/**"
    };

    private final SecurityProperties properties;
    private final RestAuthenticationEntryPoint authenticationEntryPoint;
    private final RestAccessDeniedHandler accessDeniedHandler;
    private final RevokedTokenValidator revokedTokenValidator;

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthenticationConverter jwtAuthenticationConverter) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(HttpMethod.POST, "/v1/auth/login").permitAll()
                        .requestMatchers(HttpMethod.POST, "/v1/auth/password-resets").permitAll()
                        .requestMatchers(HttpMethod.POST, "/v1/auth/password-resets/confirm").permitAll()
                        .requestMatchers(PUBLIC_PATHS).permitAll()
                        .requestMatchers(HttpMethod.GET, "/v1/exercise-videos/*").permitAll()
                        .requestMatchers(HttpMethod.POST, "/v1/users").hasRole(Role.ADMIN.name())
                        .requestMatchers(HttpMethod.POST, "/v1/members").hasRole(Role.ADMIN.name())
                        .requestMatchers(HttpMethod.GET, "/v1/members").hasRole(Role.ADMIN.name())
                        .requestMatchers(HttpMethod.GET, "/v1/members/*").hasRole(Role.ADMIN.name())
                        .requestMatchers(HttpMethod.PUT, "/v1/members/*").hasRole(Role.ADMIN.name())
                        .requestMatchers(HttpMethod.PATCH, "/v1/members/*/deactivate").hasRole(Role.ADMIN.name())
                        .requestMatchers(HttpMethod.PATCH, "/v1/members/*/credentials").hasRole(Role.ADMIN.name())
                        .requestMatchers(HttpMethod.POST, "/v1/members/*/payments").hasRole(Role.ADMIN.name())
                        .requestMatchers(HttpMethod.GET, "/v1/members/*/payments").hasRole(Role.ADMIN.name())
                        .requestMatchers(HttpMethod.PATCH, "/v1/payments/*/approve").hasRole(Role.ADMIN.name())
                        .requestMatchers(HttpMethod.POST, "/v1/categories").hasRole(Role.ADMIN.name())
                        .requestMatchers(HttpMethod.GET, "/v1/categories").hasRole(Role.ADMIN.name())
                        .requestMatchers(HttpMethod.POST, "/v1/suppliers").hasRole(Role.ADMIN.name())
                        .requestMatchers(HttpMethod.GET, "/v1/suppliers").hasRole(Role.ADMIN.name())
                        .requestMatchers(HttpMethod.POST, "/v1/products").hasRole(Role.ADMIN.name())
                        .requestMatchers(HttpMethod.GET, "/v1/products").hasRole(Role.ADMIN.name())
                        .requestMatchers(HttpMethod.GET, "/v1/products/*").hasRole(Role.ADMIN.name())
                        .requestMatchers(HttpMethod.PUT, "/v1/products/*").hasRole(Role.ADMIN.name())
                        .requestMatchers(HttpMethod.PATCH, "/v1/products/*/deactivate").hasRole(Role.ADMIN.name())
                        .requestMatchers(HttpMethod.POST, "/v1/products/*/inventory-movements").hasRole(Role.ADMIN.name())
                        .requestMatchers(HttpMethod.GET, "/v1/products/*/inventory-movements").hasRole(Role.ADMIN.name())
                        .requestMatchers(HttpMethod.POST, "/v1/sales/stock-validation").hasRole(Role.ADMIN.name())
                        .requestMatchers(HttpMethod.POST, "/v1/sales").hasRole(Role.ADMIN.name())
                        .requestMatchers(HttpMethod.GET, "/v1/sales").hasRole(Role.ADMIN.name())
                        .requestMatchers(HttpMethod.GET, "/v1/sales/*").hasRole(Role.ADMIN.name())
                        .requestMatchers(HttpMethod.GET, "/v1/cash-cuts").hasRole(Role.ADMIN.name())
                        .requestMatchers(HttpMethod.GET, "/v1/cash-cuts/export").hasRole(Role.ADMIN.name())
                        .requestMatchers(HttpMethod.POST, "/v1/members/*/account").hasRole(Role.ADMIN.name())
                        .requestMatchers(HttpMethod.POST, "/v1/coaches").hasRole(Role.ADMIN.name())
                        .requestMatchers(HttpMethod.GET, "/v1/coaches").hasRole(Role.ADMIN.name())
                        .requestMatchers(HttpMethod.GET, "/v1/schedule-blocks/occupancy").hasAnyRole(Role.ADMIN.name(), Role.SOCIO.name())
                        .requestMatchers(HttpMethod.POST, "/v1/schedule-blocks").hasRole(Role.ADMIN.name())
                        .requestMatchers(HttpMethod.GET, "/v1/schedule-blocks").hasRole(Role.ADMIN.name())
                        .requestMatchers(HttpMethod.GET, "/v1/schedule-blocks/*").hasRole(Role.ADMIN.name())
                        .requestMatchers(HttpMethod.PUT, "/v1/schedule-blocks/*").hasRole(Role.ADMIN.name())
                        .requestMatchers(HttpMethod.PATCH, "/v1/schedule-blocks/*/activate").hasRole(Role.ADMIN.name())
                        .requestMatchers(HttpMethod.PATCH, "/v1/schedule-blocks/*/deactivate").hasRole(Role.ADMIN.name())
                        .requestMatchers(HttpMethod.POST, "/v1/bookings").hasAnyRole(Role.ADMIN.name(), Role.SOCIO.name())
                        .requestMatchers(HttpMethod.GET, "/v1/bookings/me").hasRole(Role.SOCIO.name())
                        .requestMatchers(HttpMethod.PATCH, "/v1/bookings/*/cancel").hasAnyRole(Role.ADMIN.name(), Role.SOCIO.name())
                        .requestMatchers(HttpMethod.POST, "/v1/routines").hasRole(Role.ADMIN.name())
                        .requestMatchers(HttpMethod.GET, "/v1/routines").hasRole(Role.ADMIN.name())
                        .requestMatchers(HttpMethod.GET, "/v1/routines/by-day").hasAnyRole(Role.ADMIN.name(), Role.SOCIO.name())
                        .requestMatchers(HttpMethod.GET, "/v1/routines/*").hasRole(Role.ADMIN.name())
                        .requestMatchers(HttpMethod.PUT, "/v1/routines/*").hasRole(Role.ADMIN.name())
                        .requestMatchers(HttpMethod.DELETE, "/v1/routines/*").hasRole(Role.ADMIN.name())
                        .requestMatchers(HttpMethod.POST, "/v1/routines/*/exercises/*/video").hasRole(Role.ADMIN.name())
                        .requestMatchers(HttpMethod.DELETE, "/v1/routines/*/exercises/*/video").hasRole(Role.ADMIN.name())
                        .requestMatchers(HttpMethod.POST, "/v1/announcements").hasRole(Role.ADMIN.name())
                        .requestMatchers(HttpMethod.GET, "/v1/announcements").hasRole(Role.ADMIN.name())
                        .requestMatchers(HttpMethod.GET, "/v1/announcements/active").hasAnyRole(Role.ADMIN.name(), Role.SOCIO.name())
                        .requestMatchers(HttpMethod.POST, "/v1/announcements/*/dismiss").hasAnyRole(Role.ADMIN.name(), Role.SOCIO.name())
                        .requestMatchers(HttpMethod.DELETE, "/v1/announcements/*").hasRole(Role.ADMIN.name())
                        .requestMatchers(HttpMethod.POST, "/v1/check-ins").hasRole(Role.ADMIN.name())
                        .anyRequest().authenticated())
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter))
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler));
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(properties.bcryptStrength());
    }

    @Bean
    public JwtEncoder jwtEncoder() {
        return new NimbusJwtEncoder(new ImmutableSecret<>(secretKey()));
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(secretKey())
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        OAuth2TokenValidator<Jwt> validator =
                new DelegatingOAuth2TokenValidator<>(JwtValidators.createDefault(), revokedTokenValidator);
        decoder.setJwtValidator(validator);
        return decoder;
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authoritiesConverter = new JwtGrantedAuthoritiesConverter();
        authoritiesConverter.setAuthoritiesClaimName(ROLE_CLAIM);
        authoritiesConverter.setAuthorityPrefix(ROLE_AUTHORITY_PREFIX);
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);
        return converter;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(properties.allowedOrigins());
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        configuration.setMaxAge(Duration.ofHours(1));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    private SecretKey secretKey() {
        byte[] secretBytes = properties.jwtSecret().getBytes(StandardCharsets.UTF_8);
        if (secretBytes.length < MINIMUM_SECRET_BYTES) {
            throw new IllegalStateException("The JWT secret must have at least 32 bytes");
        }
        return new SecretKeySpec(secretBytes, HMAC_ALGORITHM);
    }
}
