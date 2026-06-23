package com.restaurant.payment.dto;

import jakarta.validation.constraints.Size;

/**
 * Body opcional para reembolsar un pago.
 *
 * @param reason motivo del reembolso (max 500 chars), opcional.
 *
 * @author Joshua
 * @since 1.0.0
 */
public record RefundPaymentRequest(

        @Size(max = 500, message = "{payment.refund.reason.size}")
        String reason
) {
}
