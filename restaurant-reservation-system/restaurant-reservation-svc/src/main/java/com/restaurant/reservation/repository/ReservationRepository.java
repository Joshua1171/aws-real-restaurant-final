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
 * Acceso a datos para la entidad Reservation.
 *
 * Usa DynamoDB Enhanced Client que mapea objetos Java ↔ DynamoDB sin necesidad
 * de manejar AttributeValue manualmente.
 */
@Repository
public class ReservationRepository {

    private static final String TABLE_NAME = "restaurant-reservations";

    private final DynamoDbTable<Reservation> reservationTable;

    public ReservationRepository(final DynamoDbEnhancedClient enhancedClient,
                                 @Value("${aws.dynamodb.table-name:restaurant-reservations}") final String tableName) {
        this.reservationTable = enhancedClient.table(
                tableName != null ? tableName : TABLE_NAME,
                TableSchema.fromBean(Reservation.class));
    }

    /**
     * Guarda o actualiza una reservación (upsert).
     */
    public Reservation save(final Reservation reservation) {
        reservationTable.putItem(reservation);
        return reservation;
    }

    /**
     * Busca una reservación por su clave compuesta (restaurantId + reservationDatetime).
     */
    public Optional<Reservation> findByKey(final String restaurantId, final String reservationDatetime) {
        final Key primaryKey = Key.builder()
                .partitionValue(restaurantId)
                .sortValue(reservationDatetime)
                .build();
        return Optional.ofNullable(reservationTable.getItem(primaryKey));
    }

    /**
     * Consulta todas las reservaciones de un restaurante entre dos fechas.
     * Operación muy eficiente: es un Query con PK + SK range (no un Scan).
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
     * Elimina físicamente una reservación.
     * Nota: en producción normalmente se hace soft-delete (status=CANCELLED) para auditoría.
     */
    public void delete(final String restaurantId, final String reservationDatetime) {
        final Key primaryKey = Key.builder()
                .partitionValue(restaurantId)
                .sortValue(reservationDatetime)
                .build();
        reservationTable.deleteItem(primaryKey);
    }
}
