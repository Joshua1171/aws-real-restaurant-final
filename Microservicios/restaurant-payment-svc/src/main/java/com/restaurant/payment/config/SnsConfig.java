package com.restaurant.payment.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sns.SnsClient;

/**
 * Bean del cliente SNS para publicar eventos de pago.
 *
 * @author Joshua
 * @since 1.0.0
 */
@Configuration
public class SnsConfig {

    @Value("${aws.region:us-east-1}")
    private String awsRegion;

    /**
     * @return cliente SNS configurado.
     */
    @Bean
    public SnsClient snsClient() {
        return SnsClient.builder()
                .region(Region.of(awsRegion))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }
}
