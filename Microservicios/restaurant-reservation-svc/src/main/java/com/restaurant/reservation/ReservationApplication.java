package com.restaurant.reservation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Punto de entrada del microservicio standalone de reservaciones.
 *
 * <p>Responsabilidades:</p>
 * <ul>
 *   <li>Crear, consultar, confirmar y cancelar reservaciones.</li>
 *   <li>Persistencia en DynamoDB (tabla: {@code restaurant-reservations}).</li>
 *   <li>Publicar eventos en SNS (topic: {@code restaurant-notifications}).</li>
 *   <li>Autenticacion via Amazon Cognito (User Pool: {@code restaurant-diners-pool}).</li>
 * </ul>
 *
 * <p>Puerto por defecto: 8080</p>
 *
 * @author Joshua
 * @since 1.0.0
 */
@SpringBootApplication
public class ReservationApplication {

    /**
     * Arranca el contexto de Spring Boot.
     *
     * @param args argumentos de linea de comandos (perfiles, propiedades override).
     */
    public static void main(final String[] args) {
        SpringApplication.run(ReservationApplication.class, args);
    }
}
