package com.restaurant.reservation.controller;

import com.restaurant.reservation.dto.CreateReservationRequest;
import com.restaurant.reservation.dto.ReservationResponse;
import com.restaurant.reservation.service.ReservationService;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * API REST de reservaciones.
 *
 * Autenticación:
 *   - Todos los endpoints (excepto health) requieren JWT válido de Cognito
 *   - El userId se extrae del claim "sub" del JWT (NO del body)
 *
 * Documentación OpenAPI: http://localhost:8080/swagger-ui.html
 */
@RestController
@RequestMapping("/api/v1/reservations")
@Tag(name = "Reservations", description = "Gestión de reservaciones de restaurante")
@SecurityRequirement(name = "bearerAuth")
public class ReservationController {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReservationController.class);
    private static final String JWT_CLAIM_SUB = "sub";

    private final ReservationService reservationService;

    public ReservationController(final ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @PostMapping
    @Operation(summary = "Crear una nueva reservación")
    public ResponseEntity<ReservationResponse> createReservation(
            @AuthenticationPrincipal final Jwt authenticatedJwt,
            @Valid @RequestBody final CreateReservationRequest request) {

        final String authenticatedUserId = authenticatedJwt.getClaimAsString(JWT_CLAIM_SUB);
        LOGGER.info("POST /reservations - userId={}", authenticatedUserId);

        final ReservationResponse createdReservation =
                reservationService.createReservation(authenticatedUserId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdReservation);
    }

    @GetMapping("/{restaurantId}/{reservationDatetime}")
    @Operation(summary = "Obtener una reservación por su clave compuesta")
    public ResponseEntity<ReservationResponse> getReservation(
            @PathVariable final String restaurantId,
            @PathVariable final String reservationDatetime) {
        final ReservationResponse reservation = reservationService.getReservation(restaurantId, reservationDatetime);
        return ResponseEntity.ok(reservation);
    }

    @GetMapping("/restaurant/{restaurantId}")
    @Operation(summary = "Listar reservaciones de un restaurante en un rango de fechas")
    public ResponseEntity<List<ReservationResponse>> getReservationsByRestaurant(
            @PathVariable final String restaurantId,
            @RequestParam final String startDatetime,
            @RequestParam final String endDatetime) {
        final List<ReservationResponse> reservations =
                reservationService.getReservationsByRestaurant(restaurantId, startDatetime, endDatetime);
        return ResponseEntity.ok(reservations);
    }

    @PutMapping("/{restaurantId}/{reservationDatetime}/confirm")
    @Operation(summary = "Confirmar una reservación (acción del dueño)")
    public ResponseEntity<ReservationResponse> confirmReservation(
            @PathVariable final String restaurantId,
            @PathVariable final String reservationDatetime) {
        final ReservationResponse confirmedReservation =
                reservationService.confirmReservation(restaurantId, reservationDatetime);
        return ResponseEntity.ok(confirmedReservation);
    }

    @DeleteMapping("/{restaurantId}/{reservationDatetime}")
    @Operation(summary = "Cancelar una reservación (soft-delete, cambia status a CANCELLED)")
    public ResponseEntity<ReservationResponse> cancelReservation(
            @PathVariable final String restaurantId,
            @PathVariable final String reservationDatetime) {
        final ReservationResponse cancelledReservation =
                reservationService.cancelReservation(restaurantId, reservationDatetime);
        return ResponseEntity.ok(cancelledReservation);
    }
}
