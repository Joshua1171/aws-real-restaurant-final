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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Logica de negocio del payment service.
 *
 * <p>Orquesta:</p>
 * <ol>
 *   <li>Persistencia en DynamoDB.</li>
 *   <li>Llamada a la pasarela ({@link PaymentGatewayService}).</li>
 *   <li>Publicacion de eventos en SNS via {@link PaymentEventPublisherService}.</li>
 * </ol>
 *
 * <p>Decisiones de diseno:</p>
 * <ul>
 *   <li>Si la pasarela rechaza, el pago queda en estado {@code FAILED} con
 *       {@code failureReason} y se publica {@code PAYMENT_FAILED}.</li>
 *   <li>Solo se puede capturar un pago en estado {@code PENDING}.</li>
 *   <li>Solo se puede reembolsar un pago en estado {@code CAPTURED}.</li>
 *   <li>Las transiciones invalidas lanzan {@link InvalidPaymentStateException}.</li>
 * </ul>
 *
 * @author Joshua
 * @since 1.0.0
 */
@Service
public class PaymentService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PaymentService.class);

    private final PaymentRepository paymentRepository;
    private final PaymentGatewayService paymentGateway;
    private final PaymentEventPublisherService eventPublisher;

    /**
     * @param paymentRepository repo DynamoDB.
     * @param paymentGateway    pasarela.
     * @param eventPublisher    publisher SNS.
     */
    public PaymentService(final PaymentRepository paymentRepository,
                          final PaymentGatewayService paymentGateway,
                          final PaymentEventPublisherService eventPublisher) {
        this.paymentRepository = paymentRepository;
        this.paymentGateway = paymentGateway;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Crea un intento de pago en estado {@code PENDING}.
     *
     * @param authenticatedUserId id del usuario (claim "sub" del JWT).
     * @param request             payload validado.
     * @return DTO del pago creado.
     */
    public PaymentResponse createPayment(final String authenticatedUserId,
                                         final CreatePaymentRequest request) {
        final String newPaymentId = UUID.randomUUID().toString();
        LOGGER.info("Creando intento de pago. paymentId={}, reservationId={}, userId={}, amount={} {}",
                newPaymentId, request.reservationId(), authenticatedUserId,
                request.amountCents(), request.currency());

        final Payment payment = Payment.newPending(
                newPaymentId,
                request.reservationId(),
                authenticatedUserId,
                request.restaurantId(),
                request.amountCents(),
                request.currency(),
                request.method());

        final Payment saved = paymentRepository.save(payment);
        return PaymentResponse.fromEntity(saved);
    }

    /**
     * Lee un pago por id.
     *
     * @param paymentId PK.
     * @return DTO del pago.
     * @throws PaymentNotFoundException si no existe.
     */
    public PaymentResponse getPayment(final String paymentId) {
        return paymentRepository.findById(paymentId)
                .map(PaymentResponse::fromEntity)
                .orElseThrow(() -> new PaymentNotFoundException(
                        "No existe pago con id=" + paymentId));
    }

    /**
     * Lista pagos por reservacion.
     *
     * @param reservationId id de la reserva.
     * @return lista de pagos.
     */
    public List<PaymentResponse> getPaymentsByReservation(final String reservationId) {
        return paymentRepository.findByReservationId(reservationId)
                .stream()
                .map(PaymentResponse::fromEntity)
                .toList();
    }

    /**
     * Captura (cobra) un pago previamente {@code PENDING}.
     *
     * @param paymentId PK.
     * @return DTO actualizado.
     * @throws PaymentNotFoundException        si no existe.
     * @throws InvalidPaymentStateException    si no esta en {@code PENDING}.
     * @throws PaymentGatewayException         si la pasarela rechaza.
     */
    public PaymentResponse capturePayment(final String paymentId) {
        final Payment payment = loadOrThrow(paymentId);
        ensureStatus(payment, Payment.Status.PENDING, "capture");

        try {
            final String gatewayReference = paymentGateway.capture(payment);
            payment.setGatewayReference(gatewayReference);
            payment.setStatus(Payment.Status.CAPTURED.name());
            payment.setUpdatedAt(Instant.now().toString());
            final Payment updated = paymentRepository.save(payment);
            publish(PaymentEvent.EVENT_PAYMENT_CAPTURED, updated);
            return PaymentResponse.fromEntity(updated);

        } catch (final PaymentGatewayException gatewayException) {
            payment.setStatus(Payment.Status.FAILED.name());
            payment.setFailureReason(gatewayException.getMessage());
            payment.setUpdatedAt(Instant.now().toString());
            final Payment failed = paymentRepository.save(payment);
            publish(PaymentEvent.EVENT_PAYMENT_FAILED, failed);
            throw gatewayException;
        }
    }

    /**
     * Reembolsa un pago en estado {@code CAPTURED}.
     *
     * @param paymentId PK.
     * @param request   body opcional con motivo.
     * @return DTO actualizado.
     * @throws PaymentNotFoundException     si no existe.
     * @throws InvalidPaymentStateException si no esta capturado.
     * @throws PaymentGatewayException      si la pasarela rechaza.
     */
    public PaymentResponse refundPayment(final String paymentId, final RefundPaymentRequest request) {
        final Payment payment = loadOrThrow(paymentId);
        ensureStatus(payment, Payment.Status.CAPTURED, "refund");

        final String refundReference = paymentGateway.refund(payment);
        payment.setGatewayReference(refundReference);
        payment.setStatus(Payment.Status.REFUNDED.name());
        if (request != null && request.reason() != null && !request.reason().isBlank()) {
            payment.setFailureReason(request.reason());
        }
        payment.setUpdatedAt(Instant.now().toString());
        final Payment updated = paymentRepository.save(payment);
        publish(PaymentEvent.EVENT_PAYMENT_REFUNDED, updated);
        return PaymentResponse.fromEntity(updated);
    }

    private Payment loadOrThrow(final String paymentId) {
        return paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException("No existe pago con id=" + paymentId));
    }

    private void ensureStatus(final Payment payment, final Payment.Status expected, final String operation) {
        if (!expected.name().equals(payment.getStatus())) {
            throw new InvalidPaymentStateException(
                    "No se puede ejecutar %s: estado actual=%s, requerido=%s"
                            .formatted(operation, payment.getStatus(), expected.name()));
        }
    }

    private void publish(final String eventType, final Payment payment) {
        final PaymentEvent event = new PaymentEvent(
                eventType,
                payment.getPaymentId(),
                payment.getReservationId(),
                payment.getUserId(),
                payment.getAmountCents(),
                payment.getCurrency(),
                Instant.now().toString());
        eventPublisher.publishEvent(event);
    }
}
