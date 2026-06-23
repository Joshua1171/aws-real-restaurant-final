package com.restaurant.reservation.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * DTO de entrada para crear una reservacion.
 *
 * <p>Los mensajes de error se resuelven via {@code MessageSource} usando
 * las claves declaradas con {@code {key}}, permitiendo i18n en/es.</p>
 *
 * @param restaurantId        id del restaurante donde se reserva.
 * @param reservationDatetime fecha-hora ISO-8601 (ej. {@code 2026-05-10T19:30:00Z}).
 * @param partySize           cantidad de personas (1..20).
 * @param specialRequests     peticiones especiales (max 500 chars), opcional.
 *
 * @author Joshua
 * @since 1.0.0
 */
public record CreateReservationRequest(

        @NotBlank(message = "{reservation.restaurantId.required}")
        String restaurantId,

        @NotBlank(message = "{reservation.datetime.required}")
        String reservationDatetime,

        @NotNull(message = "{reservation.partySize.required}")
        @Min(value = 1, message = "{reservation.partySize.min}")
        @Max(value = 20, message = "{reservation.partySize.max}")
        Integer partySize,

        @Size(max = 500, message = "{reservation.specialRequests.size}")
        String specialRequests
) {
}
