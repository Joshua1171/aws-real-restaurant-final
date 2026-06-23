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

@RestController
@RequestMapping("/api/v1/restaurants")
@Tag(name = "Restaurants", description = "Búsqueda y gestión de restaurantes")
public class RestaurantController {

    private static final Logger LOGGER = LoggerFactory.getLogger(RestaurantController.class);
    private static final String JWT_CLAIM_SUB = "sub";

    private final RestaurantService restaurantService;

    public RestaurantController(final RestaurantService restaurantService) {
        this.restaurantService = restaurantService;
    }

    @PostMapping
    @Operation(summary = "Crear restaurante (requiere JWT del dueño)")
    public ResponseEntity<RestaurantResponse> createRestaurant(
            @AuthenticationPrincipal final Jwt authenticatedJwt,
            @Valid @RequestBody final CreateRestaurantRequest request) {

        final String authenticatedOwnerId = authenticatedJwt.getClaimAsString(JWT_CLAIM_SUB);
        LOGGER.info("POST /restaurants - ownerId={}", authenticatedOwnerId);
        final RestaurantResponse createdRestaurant =
                restaurantService.createRestaurant(authenticatedOwnerId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdRestaurant);
    }

    @GetMapping("/{restaurantId}")
    @Operation(summary = "Obtener un restaurante por ID (público)")
    public ResponseEntity<RestaurantResponse> getRestaurant(@PathVariable final String restaurantId) {
        return ResponseEntity.ok(restaurantService.getRestaurant(restaurantId));
    }

    @GetMapping("/search")
    @Operation(summary = "Buscar restaurantes por ciudad (público, cacheado)")
    public ResponseEntity<List<RestaurantResponse>> searchRestaurantsByCity(
            @RequestParam final String city) {
        return ResponseEntity.ok(restaurantService.getRestaurantsByCity(city));
    }

    @DeleteMapping("/{restaurantId}")
    @Operation(summary = "Eliminar restaurante (requiere JWT)")
    public ResponseEntity<Void> deleteRestaurant(@PathVariable final String restaurantId) {
        restaurantService.deleteRestaurant(restaurantId);
        return ResponseEntity.noContent().build();
    }
}
