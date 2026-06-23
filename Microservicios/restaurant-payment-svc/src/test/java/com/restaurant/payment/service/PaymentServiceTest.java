package com.restaurant.payment.service;

import com.restaurant.payment.dto.CreatePaymentRequest;
import com.restaurant.payment.dto.PaymentEvent;
import com.restaurant.payment.dto.PaymentResponse;
import com.restaurant.payment.dto.RefundPaymentRequest;
import com.restaurant.payment.exception.InvalidPaymentStateException;
import com.restaurant.payment.exception.PaymentGatewayException;
import com.restaurant.payment.exception.PaymentNotFoundException;
import com.restaurant.payment.model.Payment;
import com.restaurant.payment.repository.PaymentRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests unitarios de {@link PaymentService}.
 *
 * <p>Cubre:</p>
 * <ul>
 *   <li>Crear: persiste y devuelve DTO en estado PENDING.</li>
 *   <li>Capturar: happy path -&gt; CAPTURED + evento PAYMENT_CAPTURED.</li>
 *   <li>Capturar: error de pasarela -&gt; FAILED + evento PAYMENT_FAILED y rethrow.</li>
 *   <li>Capturar: ya CAPTURED -&gt; InvalidPaymentStateException.</li>
 *   <li>Reembolsar: happy path -&gt; REFUNDED + evento PAYMENT_REFUNDED.</li>
 *   <li>Reembolsar: no existe -&gt; PaymentNotFoundException.</li>
 *   <li>Reembolsar: en PENDING -&gt; InvalidPaymentStateException.</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock private PaymentRepository paymentRepository;
    @Mock private PaymentGatewayService paymentGateway;
    @Mock private PaymentEventPublisherService eventPublisher;

    @InjectMocks
    private PaymentService paymentService;

    @Test
    @DisplayName("createPayment: persiste en PENDING y NO publica evento aun")
    void createPaymentHappyPath() {
        final CreatePaymentRequest request = new CreatePaymentRequest(
                "res-1", "rest-1", 250000L, "MXN", "CARD");
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        final PaymentResponse response = paymentService.createPayment("user-1", request);

        assertThat(response.status()).isEqualTo(Payment.Status.PENDING.name());
        assertThat(response.amountCents()).isEqualTo(250000L);
        assertThat(response.userId()).isEqualTo("user-1");
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("getPayment: no existe -> PaymentNotFoundException")
    void getPaymentNotFound() {
        when(paymentRepository.findById("p-x")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> paymentService.getPayment("p-x"))
                .isInstanceOf(PaymentNotFoundException.class);
    }

    @Test
    @DisplayName("capturePayment: happy path -> CAPTURED + evento")
    void captureHappyPath() {
        final Payment pending = pendingPayment();
        when(paymentRepository.findById("p-1")).thenReturn(Optional.of(pending));
        when(paymentGateway.capture(pending)).thenReturn("SIM-CH-abc");
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        final PaymentResponse response = paymentService.capturePayment("p-1");

        assertThat(response.status()).isEqualTo(Payment.Status.CAPTURED.name());
        assertThat(response.gatewayReference()).isEqualTo("SIM-CH-abc");

        final ArgumentCaptor<PaymentEvent> eventCaptor = ArgumentCaptor.forClass(PaymentEvent.class);
        verify(eventPublisher, times(1)).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().eventType()).isEqualTo(PaymentEvent.EVENT_PAYMENT_CAPTURED);
    }

    @Test
    @DisplayName("capturePayment: pasarela rechaza -> FAILED, evento PAYMENT_FAILED y rethrow")
    void captureGatewayRejects() {
        final Payment pending = pendingPayment();
        when(paymentRepository.findById("p-1")).thenReturn(Optional.of(pending));
        when(paymentGateway.capture(pending)).thenThrow(new PaymentGatewayException("rejected"));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        assertThatThrownBy(() -> paymentService.capturePayment("p-1"))
                .isInstanceOf(PaymentGatewayException.class);

        assertThat(pending.getStatus()).isEqualTo(Payment.Status.FAILED.name());
        assertThat(pending.getFailureReason()).isEqualTo("rejected");

        final ArgumentCaptor<PaymentEvent> eventCaptor = ArgumentCaptor.forClass(PaymentEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().eventType()).isEqualTo(PaymentEvent.EVENT_PAYMENT_FAILED);
    }

    @Test
    @DisplayName("capturePayment: ya CAPTURED -> InvalidPaymentStateException")
    void captureAlreadyCaptured() {
        final Payment captured = pendingPayment();
        captured.setStatus(Payment.Status.CAPTURED.name());
        when(paymentRepository.findById("p-1")).thenReturn(Optional.of(captured));

        assertThatThrownBy(() -> paymentService.capturePayment("p-1"))
                .isInstanceOf(InvalidPaymentStateException.class);
        verify(paymentGateway, never()).capture(any());
    }

    @Test
    @DisplayName("refundPayment: happy path -> REFUNDED + evento")
    void refundHappyPath() {
        final Payment captured = pendingPayment();
        captured.setStatus(Payment.Status.CAPTURED.name());
        captured.setGatewayReference("SIM-CH-old");
        when(paymentRepository.findById("p-1")).thenReturn(Optional.of(captured));
        when(paymentGateway.refund(captured)).thenReturn("SIM-RE-xyz");
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        final PaymentResponse response = paymentService.refundPayment("p-1", new RefundPaymentRequest("client cancelled"));

        assertThat(response.status()).isEqualTo(Payment.Status.REFUNDED.name());
        assertThat(response.gatewayReference()).isEqualTo("SIM-RE-xyz");
        assertThat(response.failureReason()).isEqualTo("client cancelled");

        final ArgumentCaptor<PaymentEvent> eventCaptor = ArgumentCaptor.forClass(PaymentEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().eventType()).isEqualTo(PaymentEvent.EVENT_PAYMENT_REFUNDED);
    }

    @Test
    @DisplayName("refundPayment: en PENDING -> InvalidPaymentStateException")
    void refundOnPending() {
        when(paymentRepository.findById("p-1")).thenReturn(Optional.of(pendingPayment()));
        assertThatThrownBy(() -> paymentService.refundPayment("p-1", null))
                .isInstanceOf(InvalidPaymentStateException.class);
        verify(paymentGateway, never()).refund(any());
    }

    @Test
    @DisplayName("refundPayment: no existe -> PaymentNotFoundException")
    void refundNotFound() {
        when(paymentRepository.findById("p-x")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> paymentService.refundPayment("p-x", null))
                .isInstanceOf(PaymentNotFoundException.class);
    }

    private Payment pendingPayment() {
        return Payment.newPending("p-1", "res-1", "user-1", "rest-1", 250000L, "MXN", "CARD");
    }
}
