package com.restaurant.notification.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.sns.SnsClient;

/**
 * Beans de los clientes AWS:
 * <ul>
 *   <li>{@code SesClient}: para email transaccional.</li>
 *   <li>{@code SnsClient}: para SMS publicado directamente a un numero.</li>
 * </ul>
 *
 * @author Joshua
 * @since 1.0.0
 */
@Configuration
public class AwsClientsConfig {

    @Value("${aws.region:us-east-1}")
    private String awsRegion;

    /**
     * @return cliente SES configurado.
     */
    @Bean
    public SesClient sesClient() {
        return SesClient.builder()
                .region(Region.of(awsRegion))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }

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
