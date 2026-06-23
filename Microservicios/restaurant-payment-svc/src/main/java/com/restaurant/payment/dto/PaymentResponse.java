package com.restaurant.payment.dto;

import com.restaurant.payment.model.Payment;

/**
 * DTO de respuesta de un pago.
 *
 * @author Joshua
 * @since 1.0.0
 */
public record PaymentResponse(
        String paymentId,
        String reservationId,
        String userId,
        String restaurantId,
        Long amountCents,
        String currency,
        String status,
        String method,
        String gatewayReference,
        String failureReason,
        String createdAt,
        String updatedAt
) {

    /**
     * Mapea una entidad {@link Payment} a su DTO de salida.
     *
     * @param payment entidad persistida.
     * @return DTO listo para serializar.
     */
    public static PaymentResponse fromEntity(final Payment payment) {
        return new PaymentResponse(
                payment.getPaymentId(),
                payment.getReservationId(),
                payment.getUserId(),
                payment.getRestaurantId(),
                payment.getAmountCents(),
                payment.getCurrency(),
                payment.getStatus(),
                payment.getMethod(),
                payment.getGatewayReference(),
                payment.getFailureReason(),
                payment.getCreatedAt(),
                payment.getUpdatedAt());
    }
}
