package com.restaurant.reservation.dto;

/**
 * Evento publicado en SNS cuando ocurre un cambio de estado en una reservacion.
 *
 * <p>Flujo del evento:</p>
 * <pre>
 *   ReservationService -&gt; SNS topic "restaurant-notifications"
 *     -&gt; SQS queue "restaurant-notifications-queue" (fan-out)
 *       -&gt; notification-svc consume y envia email/SMS
 * </pre>
 *
 * @param eventType           tipo de evento ({@code RESERVATION_CREATED}, {@code _CONFIRMED}, {@code _CANCELLED}).
 * @param reservationId       id de la reserva afectada.
 * @param restaurantId        id del restaurante.
 * @param userId              id del usuario.
 * @param reservationDatetime fecha-hora ISO-8601 de la reserva.
 * @param partySize           cantidad de personas.
 * @param timestamp           instante en que se genero el evento (ISO-8601).
 *
 * @author Joshua
 * @since 1.0.0
 */
public record NotificationEvent(
        String eventType,
        String reservationId,
        String restaurantId,
        String userId,
        String reservationDatetime,
        Integer partySize,
        String timestamp
) {

    /** Tipo de evento: nueva reserva creada. */
    public static final String EVENT_RESERVATION_CREATED = "RESERVATION_CREATED";
    /** Tipo de evento: reserva confirmada por el restaurante. */
    public static final String EVENT_RESERVATION_CONFIRMED = "RESERVATION_CONFIRMED";
    /** Tipo de evento: reserva cancelada. */
    public static final String EVENT_RESERVATION_CANCELLED = "RESERVATION_CANCELLED";
}
