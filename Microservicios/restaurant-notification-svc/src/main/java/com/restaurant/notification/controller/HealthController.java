package com.restaurant.notification.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

/**
 * Endpoint informativo basico para el ALB/ECS.
 *
 * <p>Este servicio no expone logica de negocio por HTTP -- toda la entrada
 * llega por SQS de manera asincrona.</p>
 *
 * @author Joshua
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/v1/notifications")
public class HealthController {

    /**
     * GET /api/v1/notifications/status.
     *
     * @return estado UP con timestamp.
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> status() {
        return ResponseEntity.ok(Map.of(
                "service", "restaurant-notification-svc",
                "status", "UP",
                "timestamp", Instant.now().toString()));
    }
}
