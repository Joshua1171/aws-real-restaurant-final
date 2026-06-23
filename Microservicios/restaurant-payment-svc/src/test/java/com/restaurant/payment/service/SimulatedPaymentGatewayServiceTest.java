package com.restaurant.payment.service;

import com.restaurant.payment.exception.PaymentGatewayException;
import com.restaurant.payment.model.Payment;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests del simulador de pasarela.
 */
class SimulatedPaymentGatewayServiceTest {

    @Test
    @DisplayName("capture: devuelve referencia con prefijo SIM-CH-")
    void captureReturnsSimulatedReference() {
        final SimulatedPaymentGatewayService gateway = new SimulatedPaymentGatewayService(true);
        final Payment payment = Payment.newPending("p-1", "res-1", "u", "r", 1000L, "MXN", "CARD");
        final String reference = gateway.capture(payment);
        assertThat(reference).startsWith("SIM-CH-");
    }

    @Test
    @DisplayName("capture con monto 13 lanza PaymentGatewayException")
    void captureRejectsTriggerAmount() {
        final SimulatedPaymentGatewayService gateway = new SimulatedPaymentGatewayService(true);
        final Payment payment = Payment.newPending("p-2", "res-1", "u", "r", 13L, "MXN", "CARD");
        assertThatThrownBy(() -> gateway.capture(payment))
                .isInstanceOf(PaymentGatewayException.class);
    }

    @Test
    @DisplayName("capture con monto 13 NO lanza si la simulacion esta deshabilitada")
    void captureWithSimulationDisabled() {
        final SimulatedPaymentGatewayService gateway = new SimulatedPaymentGatewayService(false);
        final Payment payment = Payment.newPending("p-3", "res-1", "u", "r", 13L, "MXN", "CARD");
        assertThat(gateway.capture(payment)).startsWith("SIM-CH-");
    }

    @Test
    @DisplayName("refund: devuelve referencia con prefijo SIM-RE-")
    void refundReturnsSimulatedReference() {
        final SimulatedPaymentGatewayService gateway = new SimulatedPaymentGatewayService(true);
        final Payment payment = Payment.newPending("p-1", "res-1", "u", "r", 1000L, "MXN", "CARD");
        payment.setStatus(Payment.Status.CAPTURED.name());
        final String reference = gateway.refund(payment);
        assertThat(reference).startsWith("SIM-RE-");
    }
}
