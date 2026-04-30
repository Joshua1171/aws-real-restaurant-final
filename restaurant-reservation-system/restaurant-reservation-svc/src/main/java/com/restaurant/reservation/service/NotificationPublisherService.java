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
 * Publicador de eventos de notificación hacia SNS.
 *
 * El topic SNS tiene suscripciones:
 *   - SQS queue "restaurant-notifications-queue" → consumido por notification-svc
 *   - Email endpoints (opcional, configurable)
 *
 * Los "MessageAttributes" permiten a SNS hacer filtering: suscripciones pueden
 * filtrar mensajes por eventType y recibir solo los que les interesan.
 */
@Service
public class NotificationPublisherService {

    private static final Logger LOGGER = LoggerFactory.getLogger(NotificationPublisherService.class);
    private static final String ATTR_EVENT_TYPE = "event_type";
    private static final String ATTR_DATA_TYPE_STRING = "String";

    private final SnsClient snsClient;
    private final ObjectMapper objectMapper;
    private final String topicArn;

    public NotificationPublisherService(final SnsClient snsClient,
                                        final ObjectMapper objectMapper,
                                        @Value("${aws.sns.topic-arn}") final String topicArn) {
        this.snsClient = snsClient;
        this.objectMapper = objectMapper;
        this.topicArn = topicArn;
    }

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
            LOGGER.info("Evento publicado en SNS. MessageId: {}, EventType: {}, ReservationId: {}",
                    publishResponse.messageId(),
                    notificationEvent.eventType(),
                    notificationEvent.reservationId());

        } catch (final JsonProcessingException jsonException) {
            LOGGER.error("No se pudo serializar el evento de notificación a JSON", jsonException);
            throw new IllegalStateException("Error serializando evento SNS", jsonException);
        } catch (final Exception publishException) {
            LOGGER.error("Falló la publicación en SNS. Topic: {}, EventType: {}",
                    topicArn, notificationEvent.eventType(), publishException);
            throw publishException;
        }
    }
}
