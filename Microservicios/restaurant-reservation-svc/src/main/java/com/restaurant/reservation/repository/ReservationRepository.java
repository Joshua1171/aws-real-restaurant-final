package com.restaurant.reservation.repository;

import com.restaurant.reservation.model.Reservation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;

import java.util.List;
import java.util.Optional;

/**
 * Acceso a datos para {@link Reservation} usando DynamoDB Enhanced Client.
 *
 * <p>El Enhanced Client mapea POJOs anotados directamente a items de DynamoDB,
 * evitando manejar {@code AttributeValue} manualmente.</p>
 *
 * @author Joshua
 * @since 1.0.0
 */
@Repository
public class ReservationRepository {

    private final DynamoDbTable<Reservation> reservationTable;

    /**
     * @param enhancedClient cliente DynamoDB Enhanced inyectado.
     * @param tableName      nombre logico de la tabla (override por config).
     */
    public ReservationRepository(final DynamoDbEnhancedClient enhancedClient,
                                 @Value("${aws.dynamodb.table-name:restaurant-reservations}") final String tableName) {
        this.reservationTable = enhancedClient.table(tableName, TableSchema.fromBean(Reservation.class));
    }

    /**
     * Upsert (insert si no existe, update si existe).
     *
     * @param reservation entidad a persistir.
     * @return la misma entidad (para encadenar).
     */
    public Reservation save(final Reservation reservation) {
        reservationTable.putItem(reservation);
        return reservation;
    }

    /**
     * Busca por la clave compuesta (PK + SK).
     *
     * @param restaurantId        partition key.
     * @param reservationDatetime sort key (ISO-8601).
     * @return reserva si existe.
     */
    public Optional<Reservation> findByKey(final String restaurantId, final String reservationDatetime) {
        final Key primaryKey = Key.builder()
                .partitionValue(restaurantId)
                .sortValue(reservationDatetime)
                .build();
        return Optional.ofNullable(reservationTable.getItem(primaryKey));
    }

    /**
     * Query eficiente sobre PK + rango de SK (no es Scan).
     *
     * @param restaurantId  partition key.
     * @param startDatetime inicio del rango (inclusive, ISO-8601).
     * @param endDatetime   fin del rango (inclusive, ISO-8601).
     * @return lista de reservas en el rango.
     */
    public List<Reservation> findByRestaurantBetweenDates(final String restaurantId,
                                                          final String startDatetime,
                                                          final String endDatetime) {
        final QueryConditional queryConditional = QueryConditional.sortBetween(
                Key.builder().partitionValue(restaurantId).sortValue(startDatetime).build(),
                Key.builder().partitionValue(restaurantId).sortValue(endDatetime).build());

        return reservationTable.query(queryConditional)
                .items()
                .stream()
                .toList();
    }

    /**
     * Eliminacion fisica (en produccion preferir soft-delete).
     *
     * @param restaurantId        partition key.
     * @param reservationDatetime sort key.
     */
    public void delete(final String restaurantId, final String reservationDatetime) {
        final Key primaryKey = Key.builder()
                .partitionValue(restaurantId)
                .sortValue(reservationDatetime)
                .build();
        reservationTable.deleteItem(primaryKey);
    }
}
