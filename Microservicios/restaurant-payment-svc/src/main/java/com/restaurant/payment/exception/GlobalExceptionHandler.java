package com.restaurant.payment.exception;

import com.restaurant.payment.dto.ApiError;
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
 * Manejador global de excepciones del payment service.
 *
 * @author Joshua
 * @since 1.0.0
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final MessageSource messageSource;

    /**
     * @param messageSource fuente i18n para titulos/detalles.
     */
    public GlobalExceptionHandler(final MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    /**
     * 404 Not Found.
     */
    @ExceptionHandler(PaymentNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(final PaymentNotFoundException ex,
                                                   final HttpServletRequest request) {
        LOGGER.warn("Pago no encontrado: {}", ex.getMessage());
        final String title = messageSource.getMessage(
                "error.payment.notfound.title", null, LocaleContextHolder.getLocale());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiError.of(HttpStatus.NOT_FOUND.value(), title, ex.getMessage(), request.getRequestURI()));
    }

    /**
     * 409 Conflict para transiciones de estado invalidas.
     */
    @ExceptionHandler(InvalidPaymentStateException.class)
    public ResponseEntity<ApiError> handleInvalidState(final InvalidPaymentStateException ex,
                                                       final HttpServletRequest request) {
        LOGGER.warn("Transicion invalida: {}", ex.getMessage());
        final String title = messageSource.getMessage(
                "error.payment.state.title", null, LocaleContextHolder.getLocale());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiError.of(HttpStatus.CONFLICT.value(), title, ex.getMessage(), request.getRequestURI()));
    }

    /**
     * 502 Bad Gateway cuando la pasarela rechaza.
     */
    @ExceptionHandler(PaymentGatewayException.class)
    public ResponseEntity<ApiError> handleGateway(final PaymentGatewayException ex,
                                                  final HttpServletRequest request) {
        LOGGER.error("Pasarela rechazo la operacion: {}", ex.getMessage(), ex);
        final String title = messageSource.getMessage(
                "error.payment.gateway.title", null, LocaleContextHolder.getLocale());
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(ApiError.of(HttpStatus.BAD_GATEWAY.value(), title, ex.getMessage(), request.getRequestURI()));
    }

    /**
     * 400 Bad Request para errores de validacion.
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
     * Catch-all 500.
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
