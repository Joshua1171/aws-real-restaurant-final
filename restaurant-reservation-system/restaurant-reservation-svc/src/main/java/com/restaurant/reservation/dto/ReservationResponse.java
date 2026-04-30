package com.restaurant.reservation.dto;

import com.restaurant.reservation.model.Reservation;

/**
 * DTO de respuesta para una reservación.
 * Usamos un record porque es inmutable y genera automáticamente equals/hashCode/toString.
 */
public record ReservationResponse(
        String reservationId,
        String restaurantId,
        String userId,
        String reservationDatetime,
        Integer partySize,
        String status,
        String specialRequests,
        String createdAt,
        String updatedAt
) {

    public static ReservationResponse fromEntity(final Reservation reservation) {
        return new ReservationResponse(
                reservation.getReservationId(),
                reservation.getRestaurantId(),
                reservation.getUserId(),
                reservation.getReservationDatetime(),
                reservation.getPartySize(),
                reservation.getStatus(),
                reservation.getSpecialRequests(),
                reservation.getCreatedAt(),
                reservation.getUpdatedAt());
    }
}
