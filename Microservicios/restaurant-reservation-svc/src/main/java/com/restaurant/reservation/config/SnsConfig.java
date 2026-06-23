package com.restaurant.reservation.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sns.SnsClient;

/**
 * Configuracion del cliente SNS.
 *
 * <p>Publica al topic {@code restaurant-notifications} cuando hay cambios
 * de estado en una reserva. El topic tiene una suscripcion fan-out a la cola
 * SQS {@code restaurant-notifications-queue}.</p>
 *
 * @author Joshua
 * @since 1.0.0
 */
@Configuration
public class SnsConfig {

    @Value("${aws.region:us-east-1}")
    private String awsRegion;

    /**
     * @return cliente SNS configurado con credenciales por defecto.
     */
    @Bean
    public SnsClient snsClient() {
        return SnsClient.builder()
                .region(Region.of(awsRegion))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }
}
