package com.restaurant.payment;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

/**
 * Smoke test del contexto Spring Boot del payment service.
 */
@SpringBootTest
@ActiveProfiles("dev")
@TestPropertySource(properties = {
        "spring.security.oauth2.resourceserver.jwt.issuer-uri=",
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration",
        "aws.sns.topic-arn=arn:aws:sns:us-east-1:000000000000:restaurant-payments-dev"
})
class ApplicationContextLoadsTest {

    @Test
    void contextLoads() {
        // Pasa si el contexto arranca.
    }
}
