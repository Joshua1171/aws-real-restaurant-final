package com.restaurant.reservation.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Configuracion de Spring Security.
 *
 * <p>Cadena de validacion:</p>
 * <ol>
 *   <li>Cliente envia {@code Authorization: Bearer &lt;jwt&gt;}.</li>
 *   <li>Spring valida el token contra el JWKs de Cognito.</li>
 *   <li>Issuer URI configurable via {@code COGNITO_ISSUER_URI}.</li>
 * </ol>
 *
 * <p>Endpoints publicos: {@code /actuator/health}, {@code /actuator/info},
 * {@code /swagger-ui/**}, {@code /v3/api-docs/**}.</p>
 *
 * @author Joshua
 * @since 1.0.0
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * @param http builder de HttpSecurity inyectado.
     * @return cadena de filtros configurada.
     * @throws Exception en errores de configuracion.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(final HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> { }));
        return http.build();
    }
}
