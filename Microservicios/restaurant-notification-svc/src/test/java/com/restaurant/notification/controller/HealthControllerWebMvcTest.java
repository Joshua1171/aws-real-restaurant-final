package com.restaurant.notification.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifica que el endpoint de status responda 200 con el shape esperado.
 */
@WebMvcTest(controllers = HealthController.class,
        excludeAutoConfiguration = {
                org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class
        })
class HealthControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void statusReturnsOk() throws Exception {
        mockMvc.perform(get("/api/v1/notifications/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.service").value("restaurant-notification-svc"))
                .andExpect(jsonPath("$.status").value("UP"));
    }
}
