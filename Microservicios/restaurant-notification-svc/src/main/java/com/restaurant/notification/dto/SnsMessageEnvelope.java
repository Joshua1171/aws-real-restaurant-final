package com.restaurant.notification.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Sobre JSON con el que SNS entrega un mensaje a SQS.
 *
 * <p>Estructura tipica:</p>
 * <pre>
 * {
 *   "Type": "Notification",
 *   "MessageId": "...",
 *   "TopicArn": "arn:aws:sns:...",
 *   "Message": "{...el JSON original publicado...}",
 *   "Timestamp": "..."
 * }
 * </pre>
 *
 * <p>Este record captura el sobre. {@code message} contiene el JSON serializado
 * de {@link NotificationEvent}. Para evitar este wrapping, activar "Raw Message
 * Delivery" en la suscripcion SNS-&gt;SQS.</p>
 *
 * @author Joshua
 * @since 1.0.0
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
