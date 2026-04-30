package com.restaurant.notification.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Cuando SNS entrega un mensaje a SQS, lo envuelve en un sobre JSON.
 *
 * Estructura del sobre:
 * {
 *   "Type": "Notification",
 *   "MessageId": "...",
 *   "TopicArn": "arn:aws:sns:...",
 *   "Message": "{...el JSON original publicado...}",
 *   "Timestamp": "...",
 *   "MessageAttributes": { "event_type": {...} }
 * }
 *
 * Este record captura el sobre; el campo "Message" es el JSON de NotificationEvent.
 *
 * Nota: Para entregar directamente (sin el sobre), se debe activar "Raw Message Delivery"
 * en la suscripción SNS→SQS. Aquí asumimos que NO está activado (comportamiento por defecto).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record SnsMessageEnvelope(

        @JsonProperty("Type") String type,
        @JsonProperty("MessageId") String messageId,
        @JsonProperty("TopicArn") String topicArn,
        @JsonProperty("Message") String message,
        @JsonProperty("Timestamp") String timestamp
) {
}
