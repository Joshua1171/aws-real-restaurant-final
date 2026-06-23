package com.restaurant.reservation.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.enhanced.DynamoDbEnhancedClient;

/**
 * Configuración del cliente DynamoDB Enhanced para el servicio de reservaciones.
 *
 * Usa DefaultCredentialsProvider que busca credenciales en este orden:
 *   1. Variables de entorno (AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY)
 *   2. Archivo ~/.aws/credentials
 *   3. IAM Role de la instancia (en ECS Fargate usa el task role)
 *
 * En producción (ECS Fargate) siempre usa el task role:
 *   restaurant-ecs-task-role (con política AmazonDynamoDBFullAccess)
 */
@Configuration
public class DynamoDbConfig {

    @Value("${aws.region:us-east-1}")
    private String awsRegion;

    @Bean
    public DynamoDbClient dynamoDbClient() {
        return DynamoDbClient.builder()
                .region(Region.of(awsRegion))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }

    @Bean
    public DynamoDbEnhancedClient dynamoDbEnhancedClient(final DynamoDbClient dynamoDbClient) {
        return DynamoDbEnhancedClient.builder()
                .dynamoDbClient(dynamoDbClient)
                .build();
    }
}
