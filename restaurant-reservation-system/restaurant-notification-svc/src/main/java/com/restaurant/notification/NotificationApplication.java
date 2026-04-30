package com.restaurant.notification;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Microservicio consumidor de eventos de SQS.
 *
 * Arquitectura:
 *   reservation-svc → SNS(restaurant-notifications)
 *     → SQS(restaurant-notifications-queue)
 *       → notification-svc [ESTE SERVICIO]
 *         → Amazon SES (email)
 *         → Amazon SNS (SMS directo, en topic secundario)
 *
 * El consumo de SQS es asíncrono con Spring Cloud AWS @SqsListener.
 * Si falla el procesamiento, SQS retorna el mensaje a la cola (automático con visibility timeout).
 * Tras N intentos fallidos, el mensaje va a la DLQ.
 *
 * Puerto: 8082
 */
@SpringBootApplication
public class NotificationApplication {

    public static void main(final String[] args) {
        SpringApplication.run(NotificationApplication.class, args);
    }
}
