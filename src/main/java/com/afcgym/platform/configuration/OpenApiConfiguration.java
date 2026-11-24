package com.afcgym.platform.configuration;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(title = "AFC Gym API", version = "v1"),
        security = @SecurityRequirement(name = OpenApiConfiguration.BEARER_SCHEME))
@SecurityScheme(
        name = OpenApiConfiguration.BEARER_SCHEME,
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT")
public class OpenApiConfiguration {

    public static final String BEARER_SCHEME = "bearerAuth";
}
