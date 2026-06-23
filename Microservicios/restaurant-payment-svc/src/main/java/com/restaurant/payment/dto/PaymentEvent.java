package com.restaurant.payment.dto;

/**
 * Evento publicado en SNS cuando cambia el estado de un pago.
 *
 * <p>Otros servicios (notification-svc, contabilidad, antifraude) pueden
 * suscribirse al topic {@code restaurant-payments} para reaccionar.</p>
 *
 * @param eventType    {@code PAYMENT_CAPTURED}, {@code PAYMENT_REFUNDED}, {@code PAYMENT_FAILED}.
 * @param paymentId    id del pago afectado.
 * @param reservationId id de la reservacion.
 * @param userId       id del usuario.
 * @param amountCents  monto.
 * @param currency     ISO-4217.
 * @param timestamp    ISO-8601.
 *
 * @author Joshua
 * @since 1.0.0
 */
public record PaymentEvent(
        String eventType,
        String paymentId,
        String reservationId,
        String userId,
        Long amountCents,
        String currency,
        String timestamp
) {

    /** Tipo: pago capturado con exito. */
    public static final String EVENT_PAYMENT_CAPTURED = "PAYMENT_CAPTURED";
    /** Tipo: pago reembolsado. */
    public static final String EVENT_PAYMENT_REFUNDED = "PAYMENT_REFUNDED";
    /** Tipo: pago rechazado por la pasarela. */
    public static final String EVENT_PAYMENT_FAILED = "PAYMENT_FAILED";
}
