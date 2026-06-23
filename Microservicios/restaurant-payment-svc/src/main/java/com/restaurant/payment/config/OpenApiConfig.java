package com.restaurant.payment.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

/**
 * Definicion OpenAPI / Swagger del payment service.
 *
 * @author Joshua
 * @since 1.0.0
 */
@Configuration
@OpenAPIDefinition(
        info = @Info(title = "Restaurant Payment Service",
                version = "1.0.0",
                description = "API REST para procesar pagos y reembolsos asociados a reservaciones."))
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT")
public class OpenApiConfig {
}
