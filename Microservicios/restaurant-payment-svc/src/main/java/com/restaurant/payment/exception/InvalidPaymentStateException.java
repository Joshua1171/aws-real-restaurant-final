package com.restaurant.payment.exception;

/**
 * Lanzada cuando se intenta una transicion de estado invalida
 * (ej. capturar un pago ya {@code REFUNDED}, reembolsar uno {@code FAILED}).
 *
 * <p>Se traduce a HTTP 409 Conflict.</p>
 *
 * @author Joshua
 * @since 1.0.0
 */
public class InvalidPaymentStateException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * @param message detalle legible para el cliente.
     */
    public InvalidPaymentStateException(final String message) {
        super(message);
    }
}
