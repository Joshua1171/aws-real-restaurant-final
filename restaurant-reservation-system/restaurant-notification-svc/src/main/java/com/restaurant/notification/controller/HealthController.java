package com.restaurant.notification.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

/**
 * Endpoint básico para el ALB/ECS.
 * El notification-svc no expone lógica de negocio por HTTP;
 * toda la entrada de trabajo viene por SQS (async).
 */
@RestController
@RequestMapping("/api/v1/notifications")
public class HealthController {

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> status() {
        return ResponseEntity.ok(Map.of(
                "service", "restaurant-notification-svc",
                "status", "UP",
                "timestamp", Instant.now().toString()));
    }
}
