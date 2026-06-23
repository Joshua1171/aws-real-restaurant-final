package com.restaurant.payment.controller;

import com.restaurant.payment.dto.CreatePaymentRequest;
import com.restaurant.payment.dto.PaymentResponse;
import com.restaurant.payment.dto.RefundPaymentRequest;
import com.restaurant.payment.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * API REST de pagos.
 *
 * <p>Todos los endpoints requieren JWT (Cognito). El {@code userId} se extrae
 * del claim {@code sub}, no del body.</p>
 *
 * @author Joshua
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/v1/payments")
@Tag(name = "Payments", description = "Gestion de pagos de reservaciones")
@SecurityRequirement(name = "bearerAuth")
public class PaymentController {

    private static final Logger LOGGER = LoggerFactory.getLogger(PaymentController.class);
    private static final String JWT_CLAIM_SUB = "sub";

    private final PaymentService paymentService;

    /**
     * @param paymentService servicio de negocio.
     */
    public PaymentController(final PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    /**
     * POST /api/v1/payments - crea un intento de pago en estado {@code PENDING}.
     */
    @PostMapping
    @Operation(summary = "Crear un intento de pago para una reservacion")
    public ResponseEntity<PaymentResponse> createPayment(
            @AuthenticationPrincipal final Jwt authenticatedJwt,
            @Valid @RequestBody final CreatePaymentRequest request) {

        final String userId = authenticatedJwt.getClaimAsString(JWT_CLAIM_SUB);
        LOGGER.info("POST /payments - userId={}, reservationId={}", userId, request.reservationId());
        final PaymentResponse created = paymentService.createPayment(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * GET /api/v1/payments/{paymentId} - lee un pago.
     */
    @GetMapping("/{paymentId}")
    @Operation(summary = "Obtener un pago por id")
    public ResponseEntity<PaymentResponse> getPayment(@PathVariable final String paymentId) {
        return ResponseEntity.ok(paymentService.getPayment(paymentId));
    }

    /**
     * GET /api/v1/payments/reservation/{reservationId} - lista pagos de una reserva.
     */
    @GetMapping("/reservation/{reservationId}")
    @Operation(summary = "Listar pagos por reservacion")
    public ResponseEntity<List<PaymentResponse>> getPaymentsByReservation(
            @PathVariable final String reservationId) {
        return ResponseEntity.ok(paymentService.getPaymentsByReservation(reservationId));
    }

    /**
     * POST /api/v1/payments/{paymentId}/capture - captura el pago en la pasarela.
     */
    @PostMapping("/{paymentId}/capture")
    @Operation(summary = "Capturar (cobrar) un pago en estado PENDING")
    public ResponseEntity<PaymentResponse> capturePayment(@PathVariable final String paymentId) {
        return ResponseEntity.ok(paymentService.capturePayment(paymentId));
    }

    /**
     * POST /api/v1/payments/{paymentId}/refund - reembolsa el pago.
     */
    @PostMapping("/{paymentId}/refund")
    @Operation(summary = "Reembolsar un pago en estado CAPTURED")
    public ResponseEntity<PaymentResponse> refundPayment(
            @PathVariable final String paymentId,
            @Valid @RequestBody(required = false) final RefundPaymentRequest request) {
        return ResponseEntity.ok(paymentService.refundPayment(paymentId, request));
    }
}
