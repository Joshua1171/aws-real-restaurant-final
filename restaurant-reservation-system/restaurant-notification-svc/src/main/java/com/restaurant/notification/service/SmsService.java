package com.restaurant.notification.service;

import com.restaurant.notification.dto.NotificationEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.MessageAttributeValue;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.PublishResponse;

import java.util.Map;

/**
 * Envío de SMS usando SNS (publicación directa a número de teléfono, sin topic).
 *
 * Importante:
 *   - SNS SMS tiene límites de spending (default $1/mes en sandbox)
 *   - Hay que subir el spending limit desde la consola: SNS → Text messaging
 *   - Algunos países requieren approval adicional
 *
 * Usamos SMSType=Transactional para priorizar entrega sobre costo.
 */
@Service
public class SmsService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SmsService.class);
    private static final String ATTR_SMS_TYPE = "AWS.SNS.SMS.SMSType";
    private static final String ATTR_DATA_TYPE_STRING = "String";
    private static final String SMS_TYPE_TRANSACTIONAL = "Transactional";

    private final SnsClient snsClient;

    public SmsService(final SnsClient snsClient) {
        this.snsClient = snsClient;
    }

    public void sendReservationSms(final String phoneNumberE164, final NotificationEvent event) {
        final String smsMessage = buildSmsMessage(event);

        final MessageAttributeValue smsTypeAttribute = MessageAttributeValue.builder()
                .dataType(ATTR_DATA_TYPE_STRING)
                .stringValue(SMS_TYPE_TRANSACTIONAL)
                .build();

        final PublishRequest publishRequest = PublishRequest.builder()
                .phoneNumber(phoneNumberE164)
                .message(smsMessage)
                .messageAttributes(Map.of(ATTR_SMS_TYPE, smsTypeAttribute))
                .build();

        try {
            final PublishResponse publishResponse = snsClient.publish(publishRequest);
            LOGGER.info("SMS enviado. MessageId: {}, to: {}",
                    publishResponse.messageId(), phoneNumberE164);
        } catch (final Exception snsException) {
            LOGGER.error("Error enviando SMS. to: {}, reservationId: {}",
                    phoneNumberE164, event.reservationId(), snsException);
            throw snsException;
        }
    }

    private String buildSmsMessage(final NotificationEvent event) {
        return switch (event.eventType()) {
            case NotificationEvent.EVENT_RESERVATION_CREATED ->
                    "Recibimos tu reserva #%s para %d personas el %s. Te avisaremos cuando sea confirmada."
                            .formatted(shortId(event.reservationId()), event.partySize(),
                                    event.reservationDatetime());
            case NotificationEvent.EVENT_RESERVATION_CONFIRMED ->
                    "✅ Tu reserva #%s fue CONFIRMADA para el %s. ¡Te esperamos!"
                            .formatted(shortId(event.reservationId()), event.reservationDatetime());
            case NotificationEvent.EVENT_RESERVATION_CANCELLED ->
                    "❌ Tu reserva #%s fue cancelada."
                            .formatted(shortId(event.reservationId()));
            default -> "Actualización de tu reserva #%s".formatted(shortId(event.reservationId()));
        };
    }

    private String shortId(final String reservationId) {
        return reservationId != null && reservationId.length() >= 8
                ? reservationId.substring(0, 8)
                : reservationId;
    }
}
