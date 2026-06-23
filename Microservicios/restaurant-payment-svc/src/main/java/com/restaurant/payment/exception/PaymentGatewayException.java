package com.restaurant.payment.exception;

/**
 * Lanzada cuando la pasarela rechaza la operacion (cobro, reembolso).
 *
 * <p>Se traduce a HTTP 502 Bad Gateway.</p>
 *
 * @author Joshua
 * @since 1.0.0
 */
public class PaymentGatewayException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * @param message detalle (puede contener el codigo de error de la pasarela).
     */
    public PaymentGatewayException(final String message) {
        super(message);
    }

    /**
     * @param message detalle.
     * @param cause   excepcion original.
     */
    public PaymentGatewayException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
