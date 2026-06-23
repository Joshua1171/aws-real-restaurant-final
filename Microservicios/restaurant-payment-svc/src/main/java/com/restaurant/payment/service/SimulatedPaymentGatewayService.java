package com.restaurant.payment.service;

import com.restaurant.payment.exception.PaymentGatewayException;
import com.restaurant.payment.model.Payment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Implementacion simulada de {@link PaymentGatewayService} para entornos
 * {@code dev} y {@code test}.
 *
 * <p>Comportamiento:</p>
 * <ul>
 *   <li>Acepta cualquier pago salvo si {@code amountCents == 13} (numero de la
 *       mala suerte) -- en cuyo caso lanza {@link PaymentGatewayException} para
 *       facilitar tests del camino de error.</li>
 *   <li>Devuelve identificadores con prefijo {@code SIM-} para que sea evidente
 *       en logs/DB que no son reales.</li>
 * </ul>
 *
 * <p>En {@code prod} se debe sustituir por una implementacion real (Stripe,
 * MercadoPago, etc.) anotada con {@code @Profile("prod")}.</p>
 *
 * @author Joshua
 * @since 1.0.0
 */
@Service
@Profile({"dev", "test", "default"})
public class SimulatedPaymentGatewayService implements PaymentGatewayService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SimulatedPaymentGatewayService.class);
    private static final long FAILURE_TRIGGER_AMOUNT = 13L;

    private final boolean failureSimulationEnabled;

    /**
     * @param failureSimulationEnabled si {@code true}, lanza excepcion para
     *                                 montos {@value #FAILURE_TRIGGER_AMOUNT}.
     */
    public SimulatedPaymentGatewayService(
            @Value("${payment.simulator.failure-trigger-enabled:true}") final boolean failureSimulationEnabled) {
        this.failureSimulationEnabled = failureSimulationEnabled;
    }

    @Override
    public String capture(final Payment payment) {
        LOGGER.info("[SIMULADOR] Capture solicitado. paymentId={}, amount={} {}",
                payment.getPaymentId(), payment.getAmountCents(), payment.getCurrency());

        rejectIfTriggerAmount(payment, "capture");

        final String reference = "SIM-CH-" + UUID.randomUUID();
        LOGGER.info("[SIMULADOR] Capture OK. reference={}", reference);
        return reference;
    }

    @Override
    public String refund(final Payment payment) {
        LOGGER.info("[SIMULADOR] Refund solicitado. paymentId={}, gatewayRef={}",
                payment.getPaymentId(), payment.getGatewayReference());

        rejectIfTriggerAmount(payment, "refund");

        final String reference = "SIM-RE-" + UUID.randomUUID();
        LOGGER.info("[SIMULADOR] Refund OK. reference={}", reference);
        return reference;
    }

    private void rejectIfTriggerAmount(final Payment payment, final String operation) {
        if (failureSimulationEnabled
                && payment.getAmountCents() != null
                && payment.getAmountCents() == FAILURE_TRIGGER_AMOUNT) {
            throw new PaymentGatewayException(
                    "Simulador rechaza %s para amount=%d (trigger de prueba)".formatted(
                            operation, FAILURE_TRIGGER_AMOUNT));
        }
    }
}
