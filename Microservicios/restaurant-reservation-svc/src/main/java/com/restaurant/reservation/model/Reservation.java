package com.restaurant.reservation.model;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

import java.time.Instant;

/**
 * Entidad de reservacion persistida en DynamoDB.
 *
 * <p>Tabla: {@code restaurant-reservations}</p>
 * <ul>
 *   <li><b>Partition Key</b>: {@code restaurant_id} (String) — agrupa reservas por restaurante.</li>
 *   <li><b>Sort Key</b>: {@code reservation_datetime} (String ISO-8601) — ordena cronologicamente.</li>
 * </ul>
 *
 * <p>DynamoDB Streams esta habilitado: cada insert/update dispara la Lambda
 * {@code stream-processor} que replica al S3 Data Lake para Athena.</p>
 *
 * <p>No usamos Lombok aqui porque DynamoDB Enhanced Client requiere getters/setters
 * publicos con anotaciones explicitas, mas faciles de leer escritos a mano.</p>
 *
 * @author Joshua
 * @since 1.0.0
 */
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

    /** Constructor por defecto requerido por DynamoDB Enhanced. */
    public Reservation() {
        // requerido por DynamoDB Enhanced Client
    }

    @DynamoDbPartitionKey
    @DynamoDbAttribute("restaurant_id")
    public String getRestaurantId() {
        return restaurantId;
    }

    public void setRestaurantId(final String restaurantId) {
        this.restaurantId = restaurantId;
    }

    @DynamoDbSortKey
    @DynamoDbAttribute("reservation_datetime")
    public String getReservationDatetime() {
        return reservationDatetime;
    }

    public void setReservationDatetime(final String reservationDatetime) {
        this.reservationDatetime = reservationDatetime;
    }

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

    @DynamoDbAttribute("party_size")
    public Integer getPartySize() {
        return partySize;
    }

    public void setPartySize(final Integer partySize) {
        this.partySize = partySize;
    }

    @DynamoDbAttribute("status")
    public String getStatus() {
        return status;
    }

    public void setStatus(final String status) {
        this.status = status;
    }

    @DynamoDbAttribute("special_requests")
    public String getSpecialRequests() {
        return specialRequests;
    }

    public void setSpecialRequests(final String specialRequests) {
        this.specialRequests = specialRequests;
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

    /** Estados validos de una reservacion. */
    public enum Status {
        /** Recien creada, esperando confirmacion del restaurante. */
        PENDING,
        /** Confirmada por el restaurante. */
        CONFIRMED,
        /** Cancelada por el usuario o el restaurante. */
        CANCELLED,
        /** El usuario asistio y la reserva concluyo. */
        COMPLETED,
        /** El usuario no se presento. */
        NO_SHOW
    }

    /**
     * Crea una nueva instancia con estado {@link Status#PENDING} y timestamps
     * inicializados al instante actual.
     *
     * @param restaurantId         id del restaurante.
     * @param reservationDatetime  fecha y hora de la reserva (ISO-8601).
     * @param reservationId        id unico de la reserva (UUID).
     * @param userId               id del usuario autenticado (claim "sub" del JWT).
     * @param partySize            cantidad de personas (1..20).
     * @param specialRequests      peticiones especiales libres (max 500 chars).
     * @return entidad lista para persistir.
     */
    public static Reservation newPending(final String restaurantId,
                                         final String reservationDatetime,
                                         final String reservationId,
                                         final String userId,
                                         final Integer partySize,
                                         final String specialRequests) {
        final Reservation reservation = new Reservation();
        final String now = Instant.now().toString();
        reservation.setRestaurantId(restaurantId);
        reservation.setReservationDatetime(reservationDatetime);
        reservation.setReservationId(reservationId);
        reservation.setUserId(userId);
        reservation.setPartySize(partySize);
        reservation.setSpecialRequests(specialRequests);
        reservation.setStatus(Status.PENDING.name());
        reservation.setCreatedAt(now);
        reservation.setUpdatedAt(now);
        return reservation;
    }
}
