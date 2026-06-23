package com.restaurant.reservation.dto;

import com.restaurant.reservation.model.Reservation;

/**
 * DTO de respuesta para una reservacion.
 *
 * <p>Es un {@code record} inmutable con un factory {@link #fromEntity(Reservation)}
 * que mapea desde la entidad de dominio.</p>
 *
 * @author Joshua
 * @since 1.0.0
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

    /**
     * Mapea una entidad {@link Reservation} a su representacion publica.
     *
     * @param reservation entidad persistida.
     * @return DTO listo para serializar a JSON.
     */
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
