package com.restaurant.reservation.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.enhanced.DynamoDbEnhancedClient;

/**
 * Configuracion del cliente DynamoDB Enhanced.
 *
 * <p>Usa {@link DefaultCredentialsProvider}, que resuelve credenciales en este orden:</p>
 * <ol>
 *   <li>Variables de entorno {@code AWS_ACCESS_KEY_ID} / {@code AWS_SECRET_ACCESS_KEY}.</li>
 *   <li>{@code ~/.aws/credentials} (perfil default o {@code AWS_PROFILE}).</li>
 *   <li>IAM Role del task (en ECS Fargate) o de la instancia (en EC2).</li>
 * </ol>
 *
 * @author Joshua
 * @since 1.0.0
 */
@Configuration
public class DynamoDbConfig {

    @Value("${aws.region:us-east-1}")
    private String awsRegion;

    /**
     * @return cliente DynamoDB de bajo nivel.
     */
    @Bean
    public DynamoDbClient dynamoDbClient() {
        return DynamoDbClient.builder()
                .region(Region.of(awsRegion))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }

    /**
     * @param dynamoDbClient cliente low-level.
     * @return cliente Enhanced que mapea POJOs a DynamoDB.
     */
    @Bean
    public DynamoDbEnhancedClient dynamoDbEnhancedClient(final DynamoDbClient dynamoDbClient) {
        return DynamoDbEnhancedClient.builder()
                .dynamoDbClient(dynamoDbClient)
                .build();
    }
}
