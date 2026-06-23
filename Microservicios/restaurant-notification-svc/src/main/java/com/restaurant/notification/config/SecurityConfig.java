package com.restaurant.notification.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Seguridad del notification-svc.
 *
 * <p>Servicio interno: solo expone health checks. Vive dentro de subredes
 * privadas, todo el trafico HTTP queda permitido para el ALB/ECS.</p>
 *
 * @author Joshua
 * @since 1.0.0
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * @param http builder.
     * @return cadena de filtros (todo permitido).
     * @throws Exception en errores de configuracion.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(final HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }
}
