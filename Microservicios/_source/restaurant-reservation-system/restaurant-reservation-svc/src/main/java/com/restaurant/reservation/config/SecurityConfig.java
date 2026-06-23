package com.restaurant.reservation.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Configuración de seguridad para el microservicio.
 *
 * Cadena de validación:
 *   1. Cliente envía JWT en header "Authorization: Bearer <token>"
 *   2. Spring Security valida el token contra el JWKs de Cognito
 *   3. URL del issuer: https://cognito-idp.us-east-1.amazonaws.com/us-east-1_ZLhzcDygK
 *   4. Si es válido, inyecta el JwtAuthenticationToken en el SecurityContext
 *
 * Endpoints públicos:
 *   - /actuator/health : para health checks del ALB
 *   - /swagger-ui/**   : documentación OpenAPI
 *   - /v3/api-docs/**  : especificación OpenAPI
 *
 * El resto requiere autenticación.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(final HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> {}));

        return http.build();
    }
}
