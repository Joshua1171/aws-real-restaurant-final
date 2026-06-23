package com.restaurant.reservation.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

import java.time.Instant;

/**
 * Entidad de reservación persistida en DynamoDB.
 *
 * Tabla: restaurant-reservations
 *   - Partition Key: restaurant_id (String) - agrupa todas las reservas por restaurante
 *   - Sort Key: reservation_datetime (String ISO-8601) - ordena cronológicamente
 *
 * Patrón de acceso principal:
 *   "Dame todas las reservas del restaurante X entre las fechas Y y Z"
 *   → query(pk=restaurant_id, between sk=start and end)
 *
 * Nota: DynamoDB Streams está habilitado. Cada insert/update dispara la Lambda
 * stream-processor que replica al S3 Data Lake para analítica con Athena.
 */
@Getter
@Setter
@NoArgsConstructor
@DynamoDbBean
public class Reservation {

    private String restaurantId;
    private String reservationDatetime;
    private String reservationId;
    private String userId;
    private Integer partySize;
    private String status;
    private String specialRequests;
    private String createdAt;
    private String updatedAt;

    @DynamoDbPartitionKey
    @DynamoDbAttribute("restaurant_id")
    public String getRestaurantId() {
        return restaurantId;
    }

    @DynamoDbSortKey
    @DynamoDbAttribute("reservation_datetime")
    public String getReservationDatetime() {
        return reservationDatetime;
    }

    @DynamoDbAttribute("reservation_id")
    public String getReservationId() {
        return reservationId;
    }

    @DynamoDbAttribute("user_id")
    public String getUserId() {
        return userId;
    }

    @DynamoDbAttribute("party_size")
    public Integer getPartySize() {
        return partySize;
    }

    @DynamoDbAttribute("status")
    public String getStatus() {
        return status;
    }

    @DynamoDbAttribute("special_requests")
    public String getSpecialRequests() {
        return specialRequests;
    }

    @DynamoDbAttribute("created_at")
    public String getCreatedAt() {
        return createdAt;
    }

    @DynamoDbAttribute("updated_at")
    public String getUpdatedAt() {
        return updatedAt;
    }

    /**
     * Estados posibles de una reservación.
     */
    public enum Status {
        PENDING,
        CONFIRMED,
        CANCELLED,
        COMPLETED,
        NO_SHOW
    }

    public static Reservation newPending(final String restaurantId,
                                         final String reservationDatetime,
                                         final String reservationId,
                                         final String userId,
                                         final Integer partySize,
                                         final String specialRequests) {
        final Reservation reservation = new Reservation();
        final String timestampNow = Instant.now().toString();
        reservation.setRestaurantId(restaurantId);
        reservation.setReservationDatetime(reservationDatetime);
        reservation.setReservationId(reservationId);
        reservation.setUserId(userId);
        reservation.setPartySize(partySize);
        reservation.setSpecialRequests(specialRequests);
        reservation.setStatus(Status.PENDING.name());
        reservation.setCreatedAt(timestampNow);
        reservation.setUpdatedAt(timestampNow);
        return reservation;
    }
}
