package com.restaurant.notification;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Microservicio standalone consumidor de eventos de notificacion.
 *
 * <p>Arquitectura:</p>
 * <pre>
 *   reservation-svc -&gt; SNS(restaurant-notifications)
 *     -&gt; SQS(restaurant-notifications-queue)
 *       -&gt; notification-svc [ESTE]
 *         -&gt; SES (email)
 *         -&gt; SNS (SMS directo)
 * </pre>
 *
 * <p>Si el procesamiento falla, SQS retorna el mensaje a la cola tras el
 * visibility timeout. Tras N reintentos pasa a la DLQ.</p>
 *
 * <p>Puerto por defecto: 8082.</p>
 *
 * @author Joshua
 * @since 1.0.0
 */
@SpringBootApplication
public class NotificationApplication {

    /**
     * Arranca el contexto de Spring Boot.
     *
     * @param args argumentos de linea de comandos.
     */
    public static void main(final String[] args) {
        SpringApplication.run(NotificationApplication.class, args);
    }
}
