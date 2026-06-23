package com.restaurant.reservation;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

/**
 * Smoke test: verifica que el contexto de Spring Boot arranca sin errores en perfil dev.
 */
@SpringBootTest
@ActiveProfiles("dev")
@TestPropertySource(properties = {
        "spring.security.oauth2.resourceserver.jwt.issuer-uri=",
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration",
        "aws.sns.topic-arn=arn:aws:sns:us-east-1:000000000000:restaurant-notifications"
})
class ApplicationContextLoadsTest {

    @Test
    void contextLoads() {
        // Spring Boot arranca el contexto, sin assertions especificas.
    }
}
