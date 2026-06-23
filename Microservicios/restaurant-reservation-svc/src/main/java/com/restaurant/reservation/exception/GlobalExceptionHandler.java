package com.restaurant.reservation.exception;

import com.restaurant.reservation.dto.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * Manejador global de excepciones del microservicio.
 *
 * <p>Mapea excepciones a respuestas HTTP estructuradas siguiendo RFC 7807 (Problem
 * Details for HTTP APIs) en una version simplificada via {@link ApiError}. Todos
 * los mensajes consultan {@code MessageSource} para soportar i18n en/es.</p>
 *
 * @author Joshua
 * @since 1.0.0
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final MessageSource messageSource;

    /**
     * @param messageSource fuente i18n para resolver claves de mensaje.
     */
    public GlobalExceptionHandler(final MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    /**
     * Maneja {@link ReservationNotFoundException} -&gt; HTTP 404.
     *
     * @param ex      excepcion lanzada.
     * @param request peticion en curso.
     * @return respuesta 404 con cuerpo {@link ApiError}.
     */
    @ExceptionHandler(ReservationNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(final ReservationNotFoundException ex,
                                                   final HttpServletRequest request) {
        LOGGER.warn("Reservacion no encontrada: {}", ex.getMessage());
        final String title = messageSource.getMessage(
                "error.reservation.notfound.title", null, LocaleContextHolder.getLocale());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiError.of(HttpStatus.NOT_FOUND.value(), title, ex.getMessage(), request.getRequestURI()));
    }

    /**
     * Maneja errores de Bean Validation -&gt; HTTP 400 con {@code fields}.
     *
     * @param ex      excepcion lanzada.
     * @param request peticion en curso.
     * @return respuesta 400 con detalles por campo.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(final MethodArgumentNotValidException ex,
                                                     final HttpServletRequest request) {
        final Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                fieldErrors.put(error.getField(), error.getDefaultMessage()));
        LOGGER.warn("Error de validacion: {}", fieldErrors);

        final String title = messageSource.getMessage(
                "error.validation.title", null, LocaleContextHolder.getLocale());
        final String detail = messageSource.getMessage(
                "error.validation.detail", null, LocaleContextHolder.getLocale());
        return ResponseEntity.badRequest()
                .body(ApiError.validation(title, detail, request.getRequestURI(), fieldErrors));
    }

    /**
     * Catch-all -&gt; HTTP 500. No se filtra el mensaje interno al cliente.
     *
     * @param ex      excepcion no esperada.
     * @param request peticion en curso.
     * @return respuesta 500 generica.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGeneric(final Exception ex, final HttpServletRequest request) {
        LOGGER.error("Error inesperado", ex);
        final String title = messageSource.getMessage(
                "error.internal.title", null, LocaleContextHolder.getLocale());
        final String detail = messageSource.getMessage(
                "error.internal.detail", null, LocaleContextHolder.getLocale());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiError.of(HttpStatus.INTERNAL_SERVER_ERROR.value(), title, detail, request.getRequestURI()));
    }
}
