package com.restaurant.reservation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Punto de entrada del microservicio de reservaciones.
 *
 * Responsabilidades:
 * - Crear, consultar y cancelar reservaciones
 * - Persistencia en DynamoDB (tabla: restaurant-reservations)
 * - Publicar eventos de confirmación en SNS (topic: restaurant-notifications)
 * - Autenticación vía Amazon Cognito (User Pool: restaurant-diners-pool)
 *
 * Puerto: 8080
 */
@SpringBootApplication
public class ReservationApplication {

    public static void main(final String[] args) {
        SpringApplication.run(ReservationApplication.class, args);
    }
}
