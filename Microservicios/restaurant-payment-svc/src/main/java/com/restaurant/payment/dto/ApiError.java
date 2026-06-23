package com.restaurant.payment.dto;

import java.time.Instant;
import java.util.Map;

/**
 * Cuerpo unico de respuesta de error (RFC 7807 simplificado).
 *
 * @param timestamp instante.
 * @param status    HTTP status.
 * @param error     titulo.
 * @param message   detalle.
 * @param path      URI.
 * @param fields    errores por campo (validacion); {@code null} si no aplica.
 *
 * @author Joshua
 * @since 1.0.0
 */
public record ApiError(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        Map<String, String> fields
) {

    /**
     * Factory para errores genericos sin {@code fields}.
     */
    public static ApiError of(final int status, final String error, final String message, final String path) {
        return new ApiError(Instant.now(), status, error, message, path, null);
    }

    /**
     * Factory para errores de validacion con {@code fields}.
     */
    public static ApiError validation(final String error,
                                      final String message,
                                      final String path,
                                      final Map<String, String> fields) {
        return new ApiError(Instant.now(), 400, error, message, path, fields);
    }
}
