package com.restaurant.reservation.exception;

/**
 * Excepción lanzada cuando se solicita una reservación que no existe.
 */
public class ReservationNotFoundException extends RuntimeException {

    public ReservationNotFoundException(final String message) {
        super(message);
    }
}
