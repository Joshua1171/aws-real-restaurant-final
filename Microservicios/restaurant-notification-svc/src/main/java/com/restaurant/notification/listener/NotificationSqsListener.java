package com.restaurant.notification.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.restaurant.notification.dto.NotificationEvent;
import com.restaurant.notification.dto.SnsMessageEnvelope;
import com.restaurant.notification.service.EmailService;
import com.restaurant.notification.service.SmsService;
import io.awspring.cloud.sqs.annotation.SqsListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Consumidor de la cola SQS de notificaciones.
 *
 * <p>Spring Cloud AWS SQS:</p>
 * <ul>
 *   <li>{@link SqsListener} hace polling automatico (long-polling).</li>
 *   <li>Si el metodo retorna sin excepcion el mensaje es ACK (eliminado).</li>
 *   <li>Si lanza excepcion se retorna a la cola tras visibility timeout.</li>
 *   <li>Tras N reintentos pasa a la DLQ (configurada en la cola).</li>
 * </ul>
 *
 * <p>Deserializacion en dos pasos: SQS -&gt; sobre SNS -&gt; evento de negocio.</p>
 *
 * @author Joshua
 * @since 1.0.0
 */
@Component
public class NotificationSqsListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(NotificationSqsListener.class);

    private final ObjectMapper objectMapper;
    private final EmailService emailService;
    private final SmsService smsService;
    private final String defaultRecipientEmail;

    /**
     * @param objectMapper          Jackson ObjectMapper.
     * @param emailService          servicio de email.
     * @param smsService            servicio de SMS (placeholder de uso).
     * @param defaultRecipientEmail email de prueba (lab) -- en prod resolver desde users.
     */
    public NotificationSqsListener(final ObjectMapper objectMapper,
                                   final EmailService emailService,
                                   final SmsService smsService,
                                   @Value("${notification.default-recipient-email:lab-test@example.com}")
                                   final String defaultRecipientEmail) {
        this.objectMapper = objectMapper;
        this.emailService = emailService;
        this.smsService = smsService;
        this.defaultRecipientEmail = defaultRecipientEmail;
    }

    /**
     * Recibe un mensaje SQS, lo deserializa y lo despacha al canal apropiado.
     *
     * @param rawSqsMessage payload crudo (string JSON del sobre SNS).
     */
    @SqsListener("${aws.sqs.queue-name:restaurant-notifications-queue}")
    public void onNotificationMessage(final String rawSqsMessage) {
        LOGGER.debug("Mensaje recibido de SQS (bytes={})", rawSqsMessage.length());
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
            LOGGER.error("Error procesando mensaje SQS. Volvera a la cola.", processingException);
            throw new RuntimeException("Fallo procesando mensaje SQS", processingException);
        }
    }

    /**
     * Decide canales segun el tipo de evento.
     *
     * <p>En produccion, el email/telefono se resuelve a partir del {@code userId}
     * consultando una tabla de usuarios. Aqui se usa un placeholder configurable.</p>
     *
     * @param notificationEvent evento ya deserializado.
     */
    private void dispatchNotification(final NotificationEvent notificationEvent) {
        switch (notificationEvent.eventType()) {
            case NotificationEvent.EVENT_RESERVATION_CREATED,
                 NotificationEvent.EVENT_RESERVATION_CONFIRMED,
                 NotificationEvent.EVENT_RESERVATION_CANCELLED ->
                    emailService.sendReservationEmail(defaultRecipientEmail, notificationEvent);
            default ->
                    LOGGER.warn("Evento desconocido ignorado: {}", notificationEvent.eventType());
        }
    }
}
