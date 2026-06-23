package com.restaurant.payment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.restaurant.payment.dto.CreatePaymentRequest;
import com.restaurant.payment.dto.PaymentResponse;
import com.restaurant.payment.exception.InvalidPaymentStateException;
import com.restaurant.payment.exception.PaymentNotFoundException;
import com.restaurant.payment.service.PaymentService;
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
 * Slice tests del controller de pagos.
 */
@WebMvcTest(PaymentController.class)
class PaymentControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PaymentService paymentService;

    @Test
    @DisplayName("POST /payments sin JWT -> 401")
    void postWithoutJwt() throws Exception {
        final CreatePaymentRequest request = new CreatePaymentRequest(
                "res-1", "rest-1", 250000L, "MXN", "CARD");
        mockMvc.perform(post("/api/v1/payments")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /payments happy path -> 201")
    void postHappyPath() throws Exception {
        final CreatePaymentRequest request = new CreatePaymentRequest(
                "res-1", "rest-1", 250000L, "MXN", "CARD");
        final PaymentResponse response = new PaymentResponse(
                "p-1", "res-1", "user-1", "rest-1", 250000L, "MXN",
                "PENDING", "CARD", null, null, "now", "now");
        when(paymentService.createPayment(anyString(), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/payments")
                        .with(jwt().jwt(token -> token.subject("user-1")))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.paymentId").value("p-1"));
    }

    @Test
    @DisplayName("POST /payments con currency invalida -> 400")
    void postBadCurrency() throws Exception {
        final CreatePaymentRequest request = new CreatePaymentRequest(
                "res-1", "rest-1", 250000L, "pesos", "CARD");
        mockMvc.perform(post("/api/v1/payments")
                        .with(jwt().jwt(token -> token.subject("user-1")))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.currency").exists());
    }

    @Test
    @DisplayName("GET /payments/{id} cuando no existe -> 404")
    void getNotFound() throws Exception {
        when(paymentService.getPayment(anyString()))
                .thenThrow(new PaymentNotFoundException("missing"));
        mockMvc.perform(get("/api/v1/payments/p-x")
                        .with(jwt().jwt(token -> token.subject("user-1"))))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /payments/{id}/capture en estado invalido -> 409")
    void captureInvalidState() throws Exception {
        when(paymentService.capturePayment(anyString()))
                .thenThrow(new InvalidPaymentStateException("estado actual=CAPTURED"));
        mockMvc.perform(post("/api/v1/payments/p-1/capture")
                        .with(jwt().jwt(token -> token.subject("user-1")))
                        .with(csrf()))
                .andExpect(status().isConflict());
    }
}
