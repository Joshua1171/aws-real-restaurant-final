package com.restaurant.notification.service;

import com.restaurant.notification.dto.NotificationEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.MessageAttributeValue;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.PublishResponse;

import java.util.Map;

/**
 * Envia SMS usando SNS (publish directo a numero, sin topic).
 *
 * <p>SNS SMS tiene limites de spending; en sandbox por defecto $1/mes.
 * Hay que subir el limite desde la consola SNS -&gt; Text messaging.</p>
 *
 * <p>Usa {@code SMSType=Transactional} para priorizar entrega sobre costo.</p>
 *
 * @author Joshua
 * @since 1.0.0
 */
@Service
public class SmsService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SmsService.class);
    private static final String ATTR_SMS_TYPE = "AWS.SNS.SMS.SMSType";
    private static final String ATTR_DATA_TYPE_STRING = "String";
    private static final String SMS_TYPE_TRANSACTIONAL = "Transactional";
    private static final int SHORT_ID_LENGTH = 8;

    private final SnsClient snsClient;
    private final MessageSource messageSource;

    /**
     * @param snsClient    cliente SNS.
     * @param messageSource fuente i18n.
     */
    public SmsService(final SnsClient snsClient, final MessageSource messageSource) {
        this.snsClient = snsClient;
        this.messageSource = messageSource;
    }

    /**
     * Envia un SMS al numero indicado en formato E.164.
     *
     * @param phoneNumberE164 numero (+5217221234567).
     * @param event           evento a notificar.
     */
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
            LOGGER.info("SMS enviado. messageId={}, to={}",
                    publishResponse.messageId(), phoneNumberE164);
        } catch (final Exception snsException) {
            LOGGER.error("Error enviando SMS. to={}, reservationId={}",
                    phoneNumberE164, event.reservationId(), snsException);
            throw snsException;
        }
    }

    private String buildSmsMessage(final NotificationEvent event) {
        final String key = switch (event.eventType()) {
            case NotificationEvent.EVENT_RESERVATION_CREATED -> "sms.created";
            case NotificationEvent.EVENT_RESERVATION_CONFIRMED -> "sms.confirmed";
            case NotificationEvent.EVENT_RESERVATION_CANCELLED -> "sms.cancelled";
            default -> "sms.default";
        };
        return messageSource.getMessage(
                key,
                new Object[]{shortId(event.reservationId()), event.partySize(), event.reservationDatetime()},
                LocaleContextHolder.getLocale());
    }

    private String shortId(final String reservationId) {
        return reservationId != null && reservationId.length() >= SHORT_ID_LENGTH
                ? reservationId.substring(0, SHORT_ID_LENGTH)
                : reservationId;
    }
}
