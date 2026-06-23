package com.restaurant.search.controller;

import com.restaurant.search.dto.CreateRestaurantRequest;
import com.restaurant.search.dto.RestaurantResponse;
import com.restaurant.search.service.RestaurantService;
import io.swagger.v3.oas.annotations.Operation;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * API REST de busqueda y gestion de restaurantes.
 *
 * <p>Lecturas (GET) son publicas. Escrituras (POST/DELETE) requieren JWT.</p>
 *
 * @author Joshua
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/v1/restaurants")
@Tag(name = "Restaurants", description = "Busqueda y gestion de restaurantes")
public class RestaurantController {

    private static final Logger LOGGER = LoggerFactory.getLogger(RestaurantController.class);
    private static final String JWT_CLAIM_SUB = "sub";

    private final RestaurantService restaurantService;

    /**
     * @param restaurantService servicio de negocio.
     */
    public RestaurantController(final RestaurantService restaurantService) {
        this.restaurantService = restaurantService;
    }

    /**
     * POST /api/v1/restaurants - Crea un restaurante. Requiere JWT del dueno.
     *
     * @param authenticatedJwt JWT.
     * @param request          payload validado.
     * @return 201 Created.
     */
    @PostMapping
    @Operation(summary = "Crear restaurante (requiere JWT del dueno)")
    public ResponseEntity<RestaurantResponse> createRestaurant(
            @AuthenticationPrincipal final Jwt authenticatedJwt,
            @Valid @RequestBody final CreateRestaurantRequest request) {

        final String authenticatedOwnerId = authenticatedJwt.getClaimAsString(JWT_CLAIM_SUB);
        LOGGER.info("POST /restaurants - ownerId={}", authenticatedOwnerId);
        final RestaurantResponse created = restaurantService.createRestaurant(authenticatedOwnerId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * GET /api/v1/restaurants/{restaurantId} - publico.
     *
     * @param restaurantId PK.
     * @return 200 OK.
     */
    @GetMapping("/{restaurantId}")
    @Operation(summary = "Obtener un restaurante por ID (publico)")
    public ResponseEntity<RestaurantResponse> getRestaurant(@PathVariable final String restaurantId) {
        return ResponseEntity.ok(restaurantService.getRestaurant(restaurantId));
    }

    /**
     * GET /api/v1/restaurants/search?city=... - publico, cacheado.
     *
     * @param city ciudad.
     * @return lista de restaurantes (posiblemente vacia).
     */
    @GetMapping("/search")
    @Operation(summary = "Buscar restaurantes por ciudad (publico, cacheado)")
    public ResponseEntity<List<RestaurantResponse>> searchRestaurantsByCity(
            @RequestParam final String city) {
        return ResponseEntity.ok(restaurantService.getRestaurantsByCity(city));
    }

    /**
     * DELETE /api/v1/restaurants/{restaurantId} - requiere JWT.
     *
     * @param restaurantId PK.
     * @return 204 No Content.
     */
    @DeleteMapping("/{restaurantId}")
    @Operation(summary = "Eliminar restaurante (requiere JWT)")
    public ResponseEntity<Void> deleteRestaurant(@PathVariable final String restaurantId) {
        restaurantService.deleteRestaurant(restaurantId);
        return ResponseEntity.noContent().build();
    }
}
