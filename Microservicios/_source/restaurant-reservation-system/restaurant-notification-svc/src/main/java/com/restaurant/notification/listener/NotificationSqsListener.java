package com.restaurant.notification.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.restaurant.notification.dto.NotificationEvent;
import com.restaurant.notification.dto.SnsMessageEnvelope;
import com.restaurant.notification.service.EmailService;
import com.restaurant.notification.service.SmsService;
import io.awspring.cloud.sqs.annotation.SqsListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Consumidor de la cola SQS "restaurant-notifications-queue".
 *
 * Funcionamiento de Spring Cloud AWS SQS:
 *   - @SqsListener hace polling automático (long-polling por defecto)
 *   - Si el método termina sin excepción: ACK → mensaje eliminado de la cola
 *   - Si lanza excepción: NACK → mensaje vuelve a la cola tras visibility timeout
 *   - Después de N reintentos (definidos en la cola): va a la DLQ
 *
 * Flujo de deserialización:
 *   SQS → String JSON del sobre SNS → SnsMessageEnvelope
 *     → .message() contiene el JSON original → NotificationEvent
 *
 * Por qué NO usamos "Raw Message Delivery":
 *   Queremos los MessageAttributes de SNS (event_type) disponibles como metadata.
 *   Con raw delivery se pierden. El costo es deserializar dos veces (mínimo).
 */
@Component
public class NotificationSqsListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(NotificationSqsListener.class);

    private final ObjectMapper objectMapper;
    private final EmailService emailService;
    private final SmsService smsService;

    public NotificationSqsListener(final ObjectMapper objectMapper,
                                   final EmailService emailService,
                                   final SmsService smsService) {
        this.objectMapper = objectMapper;
        this.emailService = emailService;
        this.smsService = smsService;
    }

    @SqsListener("${aws.sqs.queue-name:restaurant-notifications-queue}")
    public void onNotificationMessage(final String rawSqsMessage) {
        LOGGER.debug("Mensaje recibido de SQS (bytes: {})", rawSqsMessage.length());

        try {
            final SnsMessageEnvelope envelope = objectMapper.readValue(rawSqsMessage, SnsMessageEnvelope.class);
            final NotificationEvent notificationEvent = objectMapper.readValue(
                    envelope.message(), NotificationEvent.class);

            LOGGER.info("Procesando evento. type={}, reservationId={}, userId={}",
                    notificationEvent.eventType(),
                    notificationEvent.reservationId(),
                    notificationEvent.userId());

            dispatchNotification(notificationEvent);

        } catch (final Exception processingException) {
            LOGGER.error("Error procesando mensaje SQS. El mensaje volverá a la cola para reintento.",
                    processingException);
            throw new RuntimeException("Fallo procesando mensaje SQS", processingException);
        }
    }

    /**
     * Decide qué canales usar según el tipo de evento.
     *
     * En producción real, el recipient email/phone se consulta de la tabla "restaurant-users"
     * usando el userId del evento. Aquí se usa un placeholder configurable para el lab.
     */
    private void dispatchNotification(final NotificationEvent notificationEvent) {
        // En un sistema real: userService.findContact(event.userId()) → (email, phone)
        // Para el lab usamos valores por defecto/placeholders
        final String recipientEmail = "lab-test@example.com";

        switch (notificationEvent.eventType()) {
            case NotificationEvent.EVENT_RESERVATION_CREATED,
                 NotificationEvent.EVENT_RESERVATION_CONFIRMED,
                 NotificationEvent.EVENT_RESERVATION_CANCELLED ->
                    emailService.sendReservationEmail(recipientEmail, notificationEvent);
            default ->
                    LOGGER.warn("Evento desconocido ignorado: {}", notificationEvent.eventType());
        }
    }
}
