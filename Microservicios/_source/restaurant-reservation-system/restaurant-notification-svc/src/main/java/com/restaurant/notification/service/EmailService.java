package com.restaurant.notification.service;

import com.restaurant.notification.dto.NotificationEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.Body;
import software.amazon.awssdk.services.ses.model.Content;
import software.amazon.awssdk.services.ses.model.Destination;
import software.amazon.awssdk.services.ses.model.Message;
import software.amazon.awssdk.services.ses.model.SendEmailRequest;
import software.amazon.awssdk.services.ses.model.SendEmailResponse;

/**
 * Envío de emails transaccionales con Amazon SES.
 *
 * IMPORTANTE sobre SES en sandbox:
 *   - En modo sandbox (cuenta nueva) solo puedes enviar a emails VERIFICADOS
 *   - Límite: 200 emails/día, 1 por segundo
 *   - Para producción: hay que solicitar a AWS salir del sandbox (formulario)
 *
 * Para probar en lab: verificar tu email en SES console primero.
 */
@Service
public class EmailService {

    private static final Logger LOGGER = LoggerFactory.getLogger(EmailService.class);
    private static final String CHARSET_UTF8 = "UTF-8";

    private final SesClient sesClient;
    private final String senderEmail;

    public EmailService(final SesClient sesClient,
                        @Value("${aws.ses.sender-email:no-reply@restaurant-lab.com}") final String senderEmail) {
        this.sesClient = sesClient;
        this.senderEmail = senderEmail;
    }

    public void sendReservationEmail(final String recipientEmail, final NotificationEvent event) {
        final String emailSubject = buildSubject(event);
        final String emailBody = buildHtmlBody(event);

        final SendEmailRequest sendEmailRequest = SendEmailRequest.builder()
                .source(senderEmail)
                .destination(Destination.builder().toAddresses(recipientEmail).build())
                .message(Message.builder()
                        .subject(Content.builder().charset(CHARSET_UTF8).data(emailSubject).build())
                        .body(Body.builder()
                                .html(Content.builder().charset(CHARSET_UTF8).data(emailBody).build())
                                .build())
                        .build())
                .build();

        try {
            final SendEmailResponse sendEmailResponse = sesClient.sendEmail(sendEmailRequest);
            LOGGER.info("Email enviado. MessageId: {}, to: {}, eventType: {}",
                    sendEmailResponse.messageId(), recipientEmail, event.eventType());
        } catch (final Exception sesException) {
            LOGGER.error("Error enviando email via SES. to: {}, reservationId: {}",
                    recipientEmail, event.reservationId(), sesException);
            throw sesException;
        }
    }

    private String buildSubject(final NotificationEvent event) {
        return switch (event.eventType()) {
            case NotificationEvent.EVENT_RESERVATION_CREATED ->
                    "📩 Tu reservación fue recibida";
            case NotificationEvent.EVENT_RESERVATION_CONFIRMED ->
                    "✅ Tu reservación fue CONFIRMADA";
            case NotificationEvent.EVENT_RESERVATION_CANCELLED ->
                    "❌ Tu reservación fue cancelada";
            default -> "Actualización de tu reservación";
        };
    }

    private String buildHtmlBody(final NotificationEvent event) {
        return """
                <!DOCTYPE html>
                <html><body style='font-family: Arial, sans-serif;'>
                    <div style='max-width: 600px; margin: 0 auto; padding: 20px;'>
                        <h2 style='color: #ff9900;'>Restaurant Reservations</h2>
                        <p>Hola,</p>
                        <p>Tu reservación ha sido actualizada con el estado: <strong>%s</strong></p>
                        <table style='border-collapse: collapse; width: 100%%;'>
                            <tr><td style='padding: 8px;'><strong>ID Reservación:</strong></td><td>%s</td></tr>
                            <tr><td style='padding: 8px;'><strong>Restaurante:</strong></td><td>%s</td></tr>
                            <tr><td style='padding: 8px;'><strong>Fecha y hora:</strong></td><td>%s</td></tr>
                            <tr><td style='padding: 8px;'><strong>Personas:</strong></td><td>%d</td></tr>
                        </table>
                        <p style='color: #666; font-size: 12px; margin-top: 30px;'>
                            Este correo es automático, por favor no respondas.
                        </p>
                    </div>
                </body></html>
                """.formatted(
                event.eventType(),
                event.reservationId(),
                event.restaurantId(),
                event.reservationDatetime(),
                event.partySize());
    }
}
