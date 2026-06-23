package com.restaurant.reservation.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sns.SnsClient;

/**
 * Configuración del cliente SNS para publicar eventos de reservaciones.
 *
 * El servicio publicará al topic "restaurant-notifications" cuando:
 *   - Se crea una nueva reservación (para confirmación al usuario)
 *   - Se cancela una reservación
 *   - El restaurante actualiza el status
 *
 * El topic tiene suscrita la cola SQS "restaurant-notifications-queue"
 * que es consumida por el notification-svc (patrón fan-out).
 */
@Configuration
public class SnsConfig {

    @Value("${aws.region:us-east-1}")
    private String awsRegion;

    @Bean
    public SnsClient snsClient() {
        return SnsClient.builder()
                .region(Region.of(awsRegion))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }
}
