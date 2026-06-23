package com.restaurant.payment.model;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondaryPartitionKey;

import java.time.Instant;

/**
 * Entidad Payment persistida en DynamoDB.
 *
 * <p>Tabla: {@code restaurant-payments}</p>
 * <ul>
 *   <li><b>PK</b>: {@code payment_id} (String UUID).</li>
 *   <li><b>GSI</b> {@code reservation-index}: {@code reservation_id} -- permite
 *       listar pagos por reserva sin Scan.</li>
 * </ul>
 *
 * <p>El campo {@code amount_cents} se guarda como entero (centavos) para evitar
 * problemas de precision con {@code BigDecimal}/{@code double} en DynamoDB.</p>
 *
 * @author Joshua
 * @since 1.0.0
 */
@DynamoDbBean
public class Payment {

    private String paymentId;
    private String reservationId;
    private String userId;
    private String restaurantId;
    private Long amountCents;
    private String currency;
    private String status;
    private String method;
    private String gatewayReference;
    private String failureReason;
    private String createdAt;
    private String updatedAt;

    /** Constructor por defecto requerido por DynamoDB Enhanced. */
    public Payment() {
        // requerido por DynamoDB Enhanced Client
    }

    @DynamoDbPartitionKey
    @DynamoDbAttribute("payment_id")
    public String getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(final String paymentId) {
        this.paymentId = paymentId;
    }

    @DynamoDbSecondaryPartitionKey(indexNames = "reservation-index")
    @DynamoDbAttribute("reservation_id")
    public String getReservationId() {
        return reservationId;
    }

    public void setReservationId(final String reservationId) {
        this.reservationId = reservationId;
    }

    @DynamoDbAttribute("user_id")
    public String getUserId() {
        return userId;
    }

    public void setUserId(final String userId) {
        this.userId = userId;
    }

    @DynamoDbAttribute("restaurant_id")
    public String getRestaurantId() {
        return restaurantId;
    }

    public void setRestaurantId(final String restaurantId) {
        this.restaurantId = restaurantId;
    }

    @DynamoDbAttribute("amount_cents")
    public Long getAmountCents() {
        return amountCents;
    }

    public void setAmountCents(final Long amountCents) {
        this.amountCents = amountCents;
    }

    @DynamoDbAttribute("currency")
    public String getCurrency() {
        return currency;
    }

    public void setCurrency(final String currency) {
        this.currency = currency;
    }

    @DynamoDbAttribute("status")
    public String getStatus() {
        return status;
    }

    public void setStatus(final String status) {
        this.status = status;
    }

    @DynamoDbAttribute("method")
    public String getMethod() {
        return method;
    }

    public void setMethod(final String method) {
        this.method = method;
    }

    @DynamoDbAttribute("gateway_reference")
    public String getGatewayReference() {
        return gatewayReference;
    }

    public void setGatewayReference(final String gatewayReference) {
        this.gatewayReference = gatewayReference;
    }

    @DynamoDbAttribute("failure_reason")
    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(final String failureReason) {
        this.failureReason = failureReason;
    }

    @DynamoDbAttribute("created_at")
    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(final String createdAt) {
        this.createdAt = createdAt;
    }

    @DynamoDbAttribute("updated_at")
    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(final String updatedAt) {
        this.updatedAt = updatedAt;
    }

    /** Estados validos de un pago. */
    public enum Status {
        /** Intento creado, esperando captura por la pasarela. */
        PENDING,
        /** Pasarela cobro con exito. */
        CAPTURED,
        /** Pasarela rechazo el cobro. */
        FAILED,
        /** Pago capturado y luego reembolsado. */
        REFUNDED
    }

    /** Metodos soportados (placeholder; ampliar segun pasarela). */
    public enum Method {
        /** Tarjeta de credito o debito. */
        CARD,
        /** Wallet digital (PayPal, Apple Pay, etc.). */
        WALLET,
        /** Transferencia bancaria. */
        BANK_TRANSFER
    }

    /**
     * Crea un nuevo intento de pago en estado {@link Status#PENDING}.
     *
     * @param paymentId    UUID generado por el service.
     * @param reservationId id de la reservacion.
     * @param userId       id del usuario autenticado.
     * @param restaurantId id del restaurante.
     * @param amountCents  monto en centavos (no nulo, &gt; 0).
     * @param currency     codigo ISO-4217 (ej. {@code MXN}, {@code USD}).
     * @param method       metodo elegido por el cliente.
     * @return entidad lista para persistir.
     */
    public static Payment newPending(final String paymentId,
                                     final String reservationId,
                                     final String userId,
                                     final String restaurantId,
                                     final Long amountCents,
                                     final String currency,
                                     final String method) {
        final Payment payment = new Payment();
        final String now = Instant.now().toString();
        payment.setPaymentId(paymentId);
        payment.setReservationId(reservationId);
        payment.setUserId(userId);
        payment.setRestaurantId(restaurantId);
        payment.setAmountCents(amountCents);
        payment.setCurrency(currency);
        payment.setMethod(method);
        payment.setStatus(Status.PENDING.name());
        payment.setCreatedAt(now);
        payment.setUpdatedAt(now);
        return payment;
    }
}
