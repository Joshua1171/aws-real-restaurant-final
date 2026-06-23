package com.restaurant.reservation.dto;

/**
 * Evento que se publica en SNS cuando ocurre un cambio en una reservación.
 *
 * Flujo:
 *   ReservationService → SNS topic "restaurant-notifications"
 *     → SQS queue "restaurant-notifications-queue" (fan-out)
 *       → notification-svc consume y envía email/SMS
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

    public static final String EVENT_RESERVATION_CREATED = "RESERVATION_CREATED";
    public static final String EVENT_RESERVATION_CONFIRMED = "RESERVATION_CONFIRMED";
    public static final String EVENT_RESERVATION_CANCELLED = "RESERVATION_CANCELLED";
}
