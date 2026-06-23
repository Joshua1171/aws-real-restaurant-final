package com.restaurant.reservation.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.restaurant.reservation.dto.NotificationEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.MessageAttributeValue;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.PublishResponse;

import java.util.Map;

/**
 * Publica eventos de notificacion en SNS.
 *
 * <p>El topic SNS tiene suscripciones a SQS (fan-out) para que el
 * {@code notification-svc} procese asincronicamente los emails/SMS.</p>
 *
 * <p>Los {@code MessageAttributes} permiten a SNS hacer filtering por suscripcion,
 * en este caso por {@code event_type}.</p>
 *
 * @author Joshua
 * @since 1.0.0
 */
@Service
public class NotificationPublisherService {

    private static final Logger LOGGER = LoggerFactory.getLogger(NotificationPublisherService.class);
    private static final String ATTR_EVENT_TYPE = "event_type";
    private static final String ATTR_DATA_TYPE_STRING = "String";

    private final SnsClient snsClient;
    private final ObjectMapper objectMapper;
    private final String topicArn;

    /**
     * @param snsClient    cliente SNS configurado.
     * @param objectMapper Jackson ObjectMapper de la app.
     * @param topicArn     ARN del topic destino (de configuracion).
     */
    public NotificationPublisherService(final SnsClient snsClient,
                                        final ObjectMapper objectMapper,
                                        @Value("${aws.sns.topic-arn}") final String topicArn) {
        this.snsClient = snsClient;
        this.objectMapper = objectMapper;
        this.topicArn = topicArn;
    }

    /**
     * Serializa el evento a JSON y publica en el topic SNS configurado.
     *
     * @param notificationEvent evento a publicar.
     * @throws IllegalStateException si la serializacion JSON falla.
     */
    public void publishEvent(final NotificationEvent notificationEvent) {
        try {
            final String messagePayload = objectMapper.writeValueAsString(notificationEvent);

            final MessageAttributeValue eventTypeAttribute = MessageAttributeValue.builder()
                    .dataType(ATTR_DATA_TYPE_STRING)
                    .stringValue(notificationEvent.eventType())
                    .build();

            final PublishRequest publishRequest = PublishRequest.builder()
                    .topicArn(topicArn)
                    .message(messagePayload)
                    .messageAttributes(Map.of(ATTR_EVENT_TYPE, eventTypeAttribute))
                    .build();

            final PublishResponse publishResponse = snsClient.publish(publishRequest);
            LOGGER.info("Evento publicado en SNS. messageId={}, eventType={}, reservationId={}",
                    publishResponse.messageId(),
                    notificationEvent.eventType(),
                    notificationEvent.reservationId());

        } catch (final JsonProcessingException jsonException) {
            LOGGER.error("No se pudo serializar el evento de notificacion", jsonException);
            throw new IllegalStateException("Error serializando evento SNS", jsonException);
        } catch (final Exception publishException) {
            throw new IllegalStateException("Error publicando evento en SNS. topic=" + topicArn
                    + ", eventType=" + notificationEvent.eventType(), publishException);
        }
    }
}
