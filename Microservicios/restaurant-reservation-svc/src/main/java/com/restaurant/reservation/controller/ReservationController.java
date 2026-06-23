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
 * <p>Autenticacion: todos los endpoints (excepto health/swagger) requieren un JWT
 * valido emitido por Cognito. El {@code userId} se extrae del claim {@code sub}
 * del token, NO del cuerpo de la peticion.</p>
 *
 * <p>OpenAPI UI: {@code http://localhost:8080/swagger-ui.html}</p>
 *
 * @author Joshua
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/v1/reservations")
@Tag(name = "Reservations", description = "Gestion de reservaciones de restaurante")
@SecurityRequirement(name = "bearerAuth")
public class ReservationController {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReservationController.class);
    private static final String JWT_CLAIM_SUB = "sub";

    private final ReservationService reservationService;

    /**
     * @param reservationService servicio de negocio.
     */
    public ReservationController(final ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    /**
     * POST /api/v1/reservations - Crea una nueva reserva en estado PENDING.
     *
     * @param authenticatedJwt JWT del usuario autenticado.
     * @param request          payload validado.
     * @return 201 Created con la reserva creada.
     */
    @PostMapping
    @Operation(summary = "Crear una nueva reservacion")
    public ResponseEntity<ReservationResponse> createReservation(
            @AuthenticationPrincipal final Jwt authenticatedJwt,
            @Valid @RequestBody final CreateReservationRequest request) {

        final String authenticatedUserId = authenticatedJwt.getClaimAsString(JWT_CLAIM_SUB);
        LOGGER.info("POST /reservations - userId={}", authenticatedUserId);
        final ReservationResponse created = reservationService.createReservation(authenticatedUserId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * GET /api/v1/reservations/{restaurantId}/{reservationDatetime}.
     *
     * @param restaurantId        partition key.
     * @param reservationDatetime sort key (ISO-8601).
     * @return 200 OK con la reserva.
     */
    @GetMapping("/{restaurantId}/{reservationDatetime}")
    @Operation(summary = "Obtener una reservacion por su clave compuesta")
    public ResponseEntity<ReservationResponse> getReservation(
            @PathVariable final String restaurantId,
            @PathVariable final String reservationDatetime) {
        return ResponseEntity.ok(reservationService.getReservation(restaurantId, reservationDatetime));
    }

    /**
     * GET /api/v1/reservations/restaurant/{restaurantId}?startDatetime=...&amp;endDatetime=...
     *
     * @param restaurantId  partition key.
     * @param startDatetime inicio de rango ISO-8601.
     * @param endDatetime   fin de rango ISO-8601.
     * @return 200 OK con lista (posiblemente vacia).
     */
    @GetMapping("/restaurant/{restaurantId}")
    @Operation(summary = "Listar reservaciones de un restaurante en un rango de fechas")
    public ResponseEntity<List<ReservationResponse>> getReservationsByRestaurant(
            @PathVariable final String restaurantId,
            @RequestParam final String startDatetime,
            @RequestParam final String endDatetime) {
        return ResponseEntity.ok(
                reservationService.getReservationsByRestaurant(restaurantId, startDatetime, endDatetime));
    }

    /**
     * PUT /api/v1/reservations/{restaurantId}/{reservationDatetime}/confirm.
     *
     * @param restaurantId        partition key.
     * @param reservationDatetime sort key.
     * @return 200 OK con la reserva confirmada.
     */
    @PutMapping("/{restaurantId}/{reservationDatetime}/confirm")
    @Operation(summary = "Confirmar una reservacion (accion del dueno)")
    public ResponseEntity<ReservationResponse> confirmReservation(
            @PathVariable final String restaurantId,
            @PathVariable final String reservationDatetime) {
        return ResponseEntity.ok(reservationService.confirmReservation(restaurantId, reservationDatetime));
    }

    /**
     * DELETE /api/v1/reservations/{restaurantId}/{reservationDatetime}.
     * Soft-delete: cambia status a {@code CANCELLED}.
     *
     * @param restaurantId        partition key.
     * @param reservationDatetime sort key.
     * @return 200 OK con la reserva cancelada.
     */
    @DeleteMapping("/{restaurantId}/{reservationDatetime}")
    @Operation(summary = "Cancelar una reservacion (soft-delete, status=CANCELLED)")
    public ResponseEntity<ReservationResponse> cancelReservation(
            @PathVariable final String restaurantId,
            @PathVariable final String reservationDatetime) {
        return ResponseEntity.ok(reservationService.cancelReservation(restaurantId, reservationDatetime));
    }
}
