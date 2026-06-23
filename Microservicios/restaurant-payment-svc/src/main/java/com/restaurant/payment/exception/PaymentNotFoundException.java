package com.restaurant.payment.exception;

/**
 * Lanzada cuando se solicita un pago que no existe.
 *
 * @author Joshua
 * @since 1.0.0
 */
public class PaymentNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * @param message detalle legible para el cliente.
     */
    public PaymentNotFoundException(final String message) {
        super(message);
    }
}
