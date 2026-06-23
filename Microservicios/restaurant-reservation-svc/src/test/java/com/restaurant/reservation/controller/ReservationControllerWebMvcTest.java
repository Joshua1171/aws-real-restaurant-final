package com.restaurant.reservation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.restaurant.reservation.dto.CreateReservationRequest;
import com.restaurant.reservation.dto.ReservationResponse;
import com.restaurant.reservation.exception.ReservationNotFoundException;
import com.restaurant.reservation.service.ReservationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Slice tests del controller usando {@link WebMvcTest}.
 *
 * <p>Verifican el wiring HTTP, validacion, autenticacion y mapeo a JSON sin
 * arrancar el contexto completo.</p>
 */
@WebMvcTest(ReservationController.class)
class ReservationControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ReservationService reservationService;

    @Test
    @DisplayName("POST /reservations sin JWT -> 401 Unauthorized")
    void createWithoutJwtReturns401() throws Exception {
        final CreateReservationRequest request = new CreateReservationRequest(
                "rest-1", "2026-05-10T19:30:00Z", 4, null);
        mockMvc.perform(post("/api/v1/reservations")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /reservations con JWT y body valido -> 201")
    void createHappyPath() throws Exception {
        final CreateReservationRequest request = new CreateReservationRequest(
                "rest-1", "2026-05-10T19:30:00Z", 4, null);
        final ReservationResponse response = new ReservationResponse(
                "res-1", "rest-1", "user-1", "2026-05-10T19:30:00Z",
                4, "PENDING", null, "now", "now");
        when(reservationService.createReservation(anyString(), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/reservations")
                        .with(jwt().jwt(token -> token.subject("user-1")))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.reservationId").value("res-1"));
    }

    @Test
    @DisplayName("POST /reservations con partySize=0 -> 400 con campos")
    void createBadValidation() throws Exception {
        final CreateReservationRequest request = new CreateReservationRequest(
                "rest-1", "2026-05-10T19:30:00Z", 0, null);
        mockMvc.perform(post("/api/v1/reservations")
                        .with(jwt().jwt(token -> token.subject("user-1")))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.partySize").exists());
    }

    @Test
    @DisplayName("GET /reservations/{a}/{b} cuando no existe -> 404")
    void getNotFound() throws Exception {
        when(reservationService.getReservation(anyString(), anyString()))
                .thenThrow(new ReservationNotFoundException("missing"));

        mockMvc.perform(get("/api/v1/reservations/rest-1/2026-05-10T19:30:00Z")
                        .with(jwt().jwt(token -> token.subject("user-1"))))
                .andExpect(status().isNotFound());
    }
}
