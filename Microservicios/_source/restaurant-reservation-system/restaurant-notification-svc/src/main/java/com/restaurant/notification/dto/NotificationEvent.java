package com.restaurant.notification.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Evento deserializado desde el campo "Message" del sobre SNS.
 * Es idéntico al publicado por el reservation-svc.
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

    public static final String EVENT_RESERVATION_CREATED = "RESERVATION_CREATED";
    public static final String EVENT_RESERVATION_CONFIRMED = "RESERVATION_CONFIRMED";
    public static final String EVENT_RESERVATION_CANCELLED = "RESERVATION_CANCELLED";
}
