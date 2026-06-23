package com.restaurant.reservation.exception;

/**
 * Excepcion de dominio: se solicito una reservacion que no existe.
 *
 * <p>Capturada por {@link com.restaurant.reservation.exception.GlobalExceptionHandler}
 * y traducida a HTTP 404.</p>
 *
 * @author Joshua
 * @since 1.0.0
 */
public class ReservationNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * @param message detalle legible para el cliente.
     */
    public ReservationNotFoundException(final String message) {
        super(message);
    }
}
