package com.restaurant.reservation.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

/**
 * Definicion OpenAPI / Swagger.
 *
 * <p>Declara el esquema {@code bearerAuth} para que Swagger UI permita probar
 * los endpoints con un JWT de Cognito.</p>
 *
 * @author Joshua
 * @since 1.0.0
 */
@Configuration
@OpenAPIDefinition(
        info = @Info(title = "Restaurant Reservation Service",
                version = "1.0.0",
                description = "API REST para gestionar reservaciones de restaurantes."))
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT")
public class OpenApiConfig {
}
