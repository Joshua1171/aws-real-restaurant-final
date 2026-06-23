package com.restaurant.notification.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Evento deserializado desde el campo {@code Message} del sobre SNS.
 *
 * <p>Es identico al record publicado por {@code restaurant-reservation-svc}.
 * Mantener sincronizado entre servicios.</p>
 *
 * @param eventType           tipo de evento.
 * @param reservationId       id de la reserva.
 * @param restaurantId        id del restaurante.
 * @param userId              id del usuario.
 * @param reservationDatetime fecha y hora ISO-8601.
 * @param partySize           personas.
 * @param timestamp           cuando se genero el evento.
 *
 * @author Joshua
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record NotificationEvent(
        String eventType,
        String reservationId,
        String restaurantId,
        String userId,
        String reservationDatetime,
        Integer partySize,
        String timestamp
) {

    /** Tipo: nueva reserva creada. */
    public static final String EVENT_RESERVATION_CREATED = "RESERVATION_CREATED";
    /** Tipo: reserva confirmada. */
    public static final String EVENT_RESERVATION_CONFIRMED = "RESERVATION_CONFIRMED";
    /** Tipo: reserva cancelada. */
    public static final String EVENT_RESERVATION_CANCELLED = "RESERVATION_CANCELLED";
}
