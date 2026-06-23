package com.restaurant.payment.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * DTO de entrada para crear un intento de pago.
 *
 * <p>El {@code userId} se extrae del JWT, no del body.</p>
 *
 * @param reservationId id de la reservacion a pagar.
 * @param restaurantId  id del restaurante (sirve para reportes/cobros split).
 * @param amountCents   monto en centavos (positivo).
 * @param currency      codigo ISO-4217 (3 letras, mayusculas).
 * @param method        {@code CARD}, {@code WALLET} o {@code BANK_TRANSFER}.
 *
 * @author Joshua
 * @since 1.0.0
 */
public record CreatePaymentRequest(

        @NotBlank(message = "{payment.reservationId.required}")
        String reservationId,

        @NotBlank(message = "{payment.restaurantId.required}")
        String restaurantId,

        @NotNull(message = "{payment.amount.required}")
        @Min(value = 1, message = "{payment.amount.min}")
        Long amountCents,

        @NotBlank(message = "{payment.currency.required}")
        @Pattern(regexp = "[A-Z]{3}", message = "{payment.currency.format}")
        String currency,

        @NotBlank(message = "{payment.method.required}")
        @Pattern(regexp = "CARD|WALLET|BANK_TRANSFER", message = "{payment.method.invalid}")
        String method
) {
}
