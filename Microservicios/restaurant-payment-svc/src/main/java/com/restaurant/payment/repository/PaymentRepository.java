package com.restaurant.payment.repository;

import com.restaurant.payment.model.Payment;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbIndex;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Acceso a datos para {@link Payment}.
 *
 * <p>Usa el GSI {@code reservation-index} para listar pagos por reserva.</p>
 *
 * @author Joshua
 * @since 1.0.0
 */
@Repository
public class PaymentRepository {

    private static final String RESERVATION_INDEX_NAME = "reservation-index";

    private final DynamoDbTable<Payment> paymentTable;
    private final DynamoDbIndex<Payment> reservationIndex;

    /**
     * @param enhancedClient cliente DynamoDB Enhanced.
     * @param tableName      nombre logico de la tabla.
     */
    public PaymentRepository(final DynamoDbEnhancedClient enhancedClient,
                             @Value("${aws.dynamodb.table-name:restaurant-payments}") final String tableName) {
        this.paymentTable = enhancedClient.table(tableName, TableSchema.fromBean(Payment.class));
        this.reservationIndex = paymentTable.index(RESERVATION_INDEX_NAME);
    }

    /**
     * Upsert.
     *
     * @param payment entidad a persistir.
     * @return la misma entidad.
     */
    public Payment save(final Payment payment) {
        paymentTable.putItem(payment);
        return payment;
    }

    /**
     * @param paymentId PK.
     * @return entidad si existe.
     */
    public Optional<Payment> findById(final String paymentId) {
        final Key primaryKey = Key.builder().partitionValue(paymentId).build();
        return Optional.ofNullable(paymentTable.getItem(primaryKey));
    }

    /**
     * Query sobre el GSI {@code reservation-index}.
     *
     * @param reservationId id de reserva.
     * @return lista de pagos asociados (puede ser vacia).
     */
    public List<Payment> findByReservationId(final String reservationId) {
        final QueryConditional query = QueryConditional.keyEqualTo(
                Key.builder().partitionValue(reservationId).build());

        return reservationIndex.query(query)
                .stream()
                .flatMap(page -> page.items().stream())
                .collect(Collectors.toList());
    }
}
