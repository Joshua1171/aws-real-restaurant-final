package com.restaurant.search.exception;

/**
 * Lanzada cuando se solicita un restaurante que no existe.
 *
 * @author Joshua
 * @since 1.0.0
 */
public class RestaurantNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * @param message detalle legible para el cliente.
     */
    public RestaurantNotFoundException(final String message) {
        super(message);
    }
}
