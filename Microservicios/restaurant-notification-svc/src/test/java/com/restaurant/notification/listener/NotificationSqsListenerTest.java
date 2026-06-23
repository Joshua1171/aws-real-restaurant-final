package com.restaurant.notification.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.restaurant.notification.dto.NotificationEvent;
import com.restaurant.notification.service.EmailService;
import com.restaurant.notification.service.SmsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Tests del listener SQS.
 *
 * <p>Cubre el path feliz, evento desconocido (no se envia nada) y mensajes
 * malformados (relanzan excepcion para que SQS reentregue).</p>
 */
@ExtendWith(MockitoExtension.class)
class NotificationSqsListenerTest {

    @Mock private EmailService emailService;
    @Mock private SmsService smsService;

    private NotificationSqsListener listener;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        listener = new NotificationSqsListener(objectMapper, emailService, smsService, "lab-test@example.com");
    }

    @Test
    @DisplayName("Happy path: evento CREATED -> email enviado")
    void happyPathSendsEmail() throws Exception {
        final String envelope = buildEnvelope(NotificationEvent.EVENT_RESERVATION_CREATED);
        listener.onNotificationMessage(envelope);

        final ArgumentCaptor<NotificationEvent> eventCaptor = ArgumentCaptor.forClass(NotificationEvent.class);
        verify(emailService).sendReservationEmail(eq("lab-test@example.com"), eventCaptor.capture());
        assertThat(eventCaptor.getValue().eventType()).isEqualTo(NotificationEvent.EVENT_RESERVATION_CREATED);
    }

    @Test
    @DisplayName("Evento desconocido -> no se envia email")
    void unknownEventTypeIsSkipped() throws Exception {
        final String envelope = buildEnvelope("UNKNOWN_EVENT");
        listener.onNotificationMessage(envelope);
        verify(emailService, never()).sendReservationEmail(any(), any());
    }

    @Test
    @DisplayName("Mensaje malformado -> relanza para que SQS reentregue")
    void malformedMessageRethrows() {
        assertThatThrownBy(() -> listener.onNotificationMessage("no-es-json"))
                .isInstanceOf(RuntimeException.class);
    }

    private String buildEnvelope(final String eventType) throws Exception {
        final NotificationEvent event = new NotificationEvent(
                eventType, "res-1", "rest-1", "user-1", "2026-05-10T19:30:00Z", 4, "now");
        final String innerJson = objectMapper.writeValueAsString(event);

        return """
                {
                  "Type": "Notification",
                  "MessageId": "abc-123",
                  "TopicArn": "arn:aws:sns:us-east-1:0:restaurant-notifications",
                  "Message": %s,
                  "Timestamp": "now"
                }
                """.formatted(objectMapper.writeValueAsString(innerJson));
    }
}
