package com.restaurant.payment.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.restaurant.payment.dto.PaymentEvent;
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
 * Publicador de eventos de pago en SNS.
 *
 * <p>Topic: {@code restaurant-payments} (suscritores tipicos:
 * notification-svc, contabilidad, antifraude).</p>
 *
 * @author Joshua
 * @since 1.0.0
 */
@Service
public class PaymentEventPublisherService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PaymentEventPublisherService.class);
    private static final String ATTR_EVENT_TYPE = "event_type";
    private static final String ATTR_DATA_TYPE_STRING = "String";

    private final SnsClient snsClient;
    private final ObjectMapper objectMapper;
    private final String topicArn;

    /**
     * @param snsClient    cliente SNS.
     * @param objectMapper Jackson.
     * @param topicArn     ARN del topic.
     */
    public PaymentEventPublisherService(final SnsClient snsClient,
                                        final ObjectMapper objectMapper,
                                        @Value("${aws.sns.topic-arn}") final String topicArn) {
        this.snsClient = snsClient;
        this.objectMapper = objectMapper;
        this.topicArn = topicArn;
    }

    /**
     * Publica un evento serializado a JSON con {@code MessageAttribute event_type}.
     *
     * @param event evento a publicar.
     * @throws IllegalStateException si la serializacion falla.
     */
    public void publishEvent(final PaymentEvent event) {
        try {
            final String payload = objectMapper.writeValueAsString(event);

            final MessageAttributeValue eventTypeAttribute = MessageAttributeValue.builder()
                    .dataType(ATTR_DATA_TYPE_STRING)
                    .stringValue(event.eventType())
                    .build();

            final PublishRequest request = PublishRequest.builder()
                    .topicArn(topicArn)
                    .message(payload)
                    .messageAttributes(Map.of(ATTR_EVENT_TYPE, eventTypeAttribute))
                    .build();

            final PublishResponse response = snsClient.publish(request);
            LOGGER.info("Evento publicado en SNS. messageId={}, eventType={}, paymentId={}",
                    response.messageId(), event.eventType(), event.paymentId());

        } catch (final JsonProcessingException jsonException) {
            LOGGER.error("No se pudo serializar el evento de pago", jsonException);
            throw new IllegalStateException("Error serializando evento SNS", jsonException);
        } catch (final Exception publishException) {
            LOGGER.error("Fallo la publicacion en SNS. topic={}, eventType={}",
                    topicArn, event.eventType(), publishException);
            throw publishException;
        }
    }
}
