package com.restaurant.payment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Punto de entrada del microservicio standalone de pagos.
 *
 * <p>Responsabilidades:</p>
 * <ul>
 *   <li>Crear intentos de pago asociados a una reservacion.</li>
 *   <li>Capturar (cobrar) un pago previamente autorizado.</li>
 *   <li>Reembolsar un pago capturado cuando una reserva se cancela.</li>
 *   <li>Persistir los pagos en DynamoDB ({@code restaurant-payments}).</li>
 *   <li>Publicar eventos en SNS ({@code restaurant-payments}) cuando cambia el estado.</li>
 * </ul>
 *
 * <p>La integracion con la pasarela real (Stripe / MercadoPago / etc.) esta
 * detras de la abstraccion {@code PaymentGatewayService}, que en {@code dev}
 * usa una implementacion simulada y en {@code prod} se reemplaza por la real.</p>
 *
 * <p>Puerto por defecto: 8083.</p>
 *
 * @author Joshua
 * @since 1.0.0
 */
@SpringBootApplication
public class PaymentApplication {

    /**
     * Arranca el contexto de Spring Boot.
     *
     * @param args argumentos de linea de comandos (perfiles, overrides).
     */
    public static void main(final String[] args) {
        SpringApplication.run(PaymentApplication.class, args);
    }
}
