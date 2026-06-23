package com.restaurant.payment.service;

import com.restaurant.payment.exception.PaymentGatewayException;
import com.restaurant.payment.model.Payment;

/**
 * Abstraccion sobre la pasarela de pagos (Stripe / MercadoPago / etc.).
 *
 * <p>El servicio de aplicacion consume esta interfaz; la implementacion real
 * se inyecta segun el perfil:</p>
 * <ul>
 *   <li>{@code dev} / {@code test}: simulador en memoria.</li>
 *   <li>{@code prod}: implementacion real (no incluida en este lab).</li>
 * </ul>
 *
 * @author Joshua
 * @since 1.0.0
 */
public interface PaymentGatewayService {

    /**
     * Cobra el monto del pago en la pasarela.
     *
     * @param payment entidad ya persistida en estado {@code PENDING}.
     * @return identificador de la transaccion en la pasarela
     *         (ej. {@code ch_1ABC2D...}).
     * @throws PaymentGatewayException si la pasarela rechaza.
     */
    String capture(Payment payment);

    /**
     * Reembolsa el pago en la pasarela.
     *
     * @param payment entidad ya capturada con {@code gatewayReference}.
     * @return identificador del reembolso en la pasarela.
     * @throws PaymentGatewayException si la pasarela rechaza.
     */
    String refund(Payment payment);
}
