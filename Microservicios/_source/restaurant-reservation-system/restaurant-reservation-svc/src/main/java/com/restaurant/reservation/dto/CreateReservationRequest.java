package com.restaurant.reservation.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * DTO de entrada para crear una reservación.
 * La validación de Bean Validation (Jakarta) se aplica automáticamente con @Valid.
 */
public record CreateReservationRequest(

        @NotBlank(message = "El ID del restaurante es obligatorio")
        String restaurantId,

        @NotBlank(message = "La fecha y hora de la reservación es obligatoria (formato ISO-8601)")
        String reservationDatetime,

        @NotNull(message = "El tamaño del grupo es obligatorio")
        @Min(value = 1, message = "El grupo debe tener al menos 1 persona")
        @Max(value = 20, message = "El grupo no puede exceder 20 personas")
        Integer partySize,

        @Size(max = 500, message = "Las peticiones especiales no pueden exceder 500 caracteres")
        String specialRequests
) {
}
