package com.restaurant.notification.service;

import com.restaurant.notification.dto.NotificationEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.Body;
import software.amazon.awssdk.services.ses.model.Content;
import software.amazon.awssdk.services.ses.model.Destination;
import software.amazon.awssdk.services.ses.model.Message;
import software.amazon.awssdk.services.ses.model.SendEmailRequest;
import software.amazon.awssdk.services.ses.model.SendEmailResponse;

/**
 * Envia emails transaccionales con Amazon SES.
 *
 * <p>SES en sandbox: solo enviar a destinatarios verificados; limite 200/dia
 * y 1 por segundo. Para produccion solicitar salir del sandbox.</p>
 *
 * @author Joshua
 * @since 1.0.0
 */
@Service
public class EmailService {

    private static final Logger LOGGER = LoggerFactory.getLogger(EmailService.class);
    private static final String CHARSET_UTF8 = "UTF-8";

    private final SesClient sesClient;
    private final MessageSource messageSource;
    private final String senderEmail;

    /**
     * @param sesClient     cliente SES.
     * @param messageSource fuente i18n para subject/body.
     * @param senderEmail   email remitente (verificado en SES).
     */
    public EmailService(final SesClient sesClient,
                        final MessageSource messageSource,
                        @Value("${aws.ses.sender-email:no-reply@restaurant-lab.com}") final String senderEmail) {
        this.sesClient = sesClient;
        this.messageSource = messageSource;
        this.senderEmail = senderEmail;
    }

    /**
     * Construye y envia el email asociado al evento.
     *
     * @param recipientEmail destinatario.
     * @param event          evento a notificar.
     */
    public void sendReservationEmail(final String recipientEmail, final NotificationEvent event) {
        final String subject = buildSubject(event);
        final String body = buildHtmlBody(event);

        final SendEmailRequest sendEmailRequest = SendEmailRequest.builder()
                .source(senderEmail)
                .destination(Destination.builder().toAddresses(recipientEmail).build())
                .message(Message.builder()
                        .subject(Content.builder().charset(CHARSET_UTF8).data(subject).build())
                        .body(Body.builder()
                                .html(Content.builder().charset(CHARSET_UTF8).data(body).build())
                                .build())
                        .build())
                .build();

        try {
            final SendEmailResponse sendEmailResponse = sesClient.sendEmail(sendEmailRequest);
            LOGGER.info("Email enviado. messageId={}, to={}, eventType={}",
                    sendEmailResponse.messageId(), recipientEmail, event.eventType());
        } catch (final Exception sesException) {
            LOGGER.error("Error enviando email via SES. to={}, reservationId={}",
                    recipientEmail, event.reservationId(), sesException);
            throw sesException;
        }
    }

    private String buildSubject(final NotificationEvent event) {
        final String key = switch (event.eventType()) {
            case NotificationEvent.EVENT_RESERVATION_CREATED -> "email.subject.created";
            case NotificationEvent.EVENT_RESERVATION_CONFIRMED -> "email.subject.confirmed";
            case NotificationEvent.EVENT_RESERVATION_CANCELLED -> "email.subject.cancelled";
            default -> "email.subject.default";
        };
        return messageSource.getMessage(key, null, LocaleContextHolder.getLocale());
    }

    private String buildHtmlBody(final NotificationEvent event) {
        final String header = messageSource.getMessage(
                "email.body.header", null, LocaleContextHolder.getLocale());
        final String greeting = messageSource.getMessage(
                "email.body.greeting", null, LocaleContextHolder.getLocale());
        final String statusLine = messageSource.getMessage(
                "email.body.status", new Object[]{event.eventType()}, LocaleContextHolder.getLocale());
        final String footer = messageSource.getMessage(
                "email.body.footer", null, LocaleContextHolder.getLocale());

        return """
                <!DOCTYPE html>
                <html><body style='font-family: Arial, sans-serif;'>
                    <div style='max-width: 600px; margin: 0 auto; padding: 20px;'>
                        <h2 style='color: #ff9900;'>%s</h2>
                        <p>%s</p>
                        <p>%s</p>
                        <table style='border-collapse: collapse; width: 100%%;'>
                            <tr><td style='padding: 8px;'><strong>Reservation:</strong></td><td>%s</td></tr>
                            <tr><td style='padding: 8px;'><strong>Restaurant:</strong></td><td>%s</td></tr>
                            <tr><td style='padding: 8px;'><strong>Datetime:</strong></td><td>%s</td></tr>
                            <tr><td style='padding: 8px;'><strong>Party size:</strong></td><td>%d</td></tr>
                        </table>
                        <p style='color: #666; font-size: 12px; margin-top: 30px;'>%s</p>
                    </div>
                </body></html>
                """.formatted(
                header,
                greeting,
                statusLine,
                event.reservationId(),
                event.restaurantId(),
                event.reservationDatetime(),
                event.partySize() != null ? event.partySize() : 0,
                footer);
    }
}
