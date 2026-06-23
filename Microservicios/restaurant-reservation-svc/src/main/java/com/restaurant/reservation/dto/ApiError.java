package com.restaurant.reservation.dto;

import java.time.Instant;
import java.util.Map;

/**
 * Cuerpo estandar de respuesta de error siguiendo {@code application/problem+json}
 * (RFC 7807) en una version simplificada.
 *
 * @param timestamp instante del error (ISO-8601).
 * @param status    codigo HTTP.
 * @param error     titulo corto y legible.
 * @param message   detalle accionable para el cliente.
 * @param path      URI de la peticion que provoco el error.
 * @param fields    errores por campo en validaciones; {@code null} si no aplica.
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
     * Construye un error generico (sin {@code fields}).
     *
     * @param status  HTTP status.
     * @param error   titulo.
     * @param message detalle.
     * @param path    URI de la peticion.
     * @return error listo para serializar.
     */
    public static ApiError of(final int status, final String error, final String message, final String path) {
        return new ApiError(Instant.now(), status, error, message, path, null);
    }

    /**
     * Construye un error de validacion con {@code fields}.
     *
     * @param error   titulo.
     * @param message detalle.
     * @param path    URI de la peticion.
     * @param fields  mapa campo -&gt; mensaje de validacion.
     * @return error listo para serializar.
     */
    public static ApiError validation(final String error,
                                      final String message,
                                      final String path,
                                      final Map<String, String> fields) {
        return new ApiError(Instant.now(), 400, error, message, path, fields);
    }
}
