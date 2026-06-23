package com.restaurant.reservation.service;

import com.restaurant.reservation.dto.CreateReservationRequest;
import com.restaurant.reservation.dto.NotificationEvent;
import com.restaurant.reservation.dto.ReservationResponse;
import com.restaurant.reservation.exception.ReservationNotFoundException;
import com.restaurant.reservation.model.Reservation;
import com.restaurant.reservation.repository.ReservationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Logica de negocio de reservaciones.
 *
 * <p>Flujo {@code createReservation}:</p>
 * <ol>
 *   <li>Genera un {@code reservationId} unico (UUID).</li>
 *   <li>Construye la entidad con estado {@code PENDING}.</li>
 *   <li>Persiste en DynamoDB (DynamoDB Stream notifica a Lambda -&gt; S3).</li>
 *   <li>Publica evento {@code RESERVATION_CREATED} en SNS.</li>
 *   <li>Devuelve el DTO al controller.</li>
 * </ol>
 *
 * @author Joshua
 * @since 1.0.0
 */
@Service
public class ReservationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReservationService.class);

    private final ReservationRepository reservationRepository;
    private final NotificationPublisherService notificationPublisher;

    /**
     * @param reservationRepository repo DynamoDB.
     * @param notificationPublisher publisher SNS.
     */
    public ReservationService(final ReservationRepository reservationRepository,
                              final NotificationPublisherService notificationPublisher) {
        this.reservationRepository = reservationRepository;
        this.notificationPublisher = notificationPublisher;
    }

    /**
     * Crea una nueva reserva en estado {@code PENDING} y publica el evento.
     *
     * @param authenticatedUserId id del usuario (del JWT, claim "sub").
     * @param request             payload validado de la peticion.
     * @return DTO de la reserva creada.
     */
    public ReservationResponse createReservation(final String authenticatedUserId,
                                                 final CreateReservationRequest request) {
        final String newReservationId = UUID.randomUUID().toString();
        LOGGER.info("Creando reservacion. userId={}, restaurantId={}, datetime={}",
                authenticatedUserId, request.restaurantId(), request.reservationDatetime());

        final Reservation reservation = Reservation.newPending(
                request.restaurantId(),
                request.reservationDatetime(),
                newReservationId,
                authenticatedUserId,
                request.partySize(),
                request.specialRequests());

        final Reservation savedReservation = reservationRepository.save(reservation);
        publish(NotificationEvent.EVENT_RESERVATION_CREATED, savedReservation);
        return ReservationResponse.fromEntity(savedReservation);
    }

    /**
     * Lee una reserva por su clave compuesta.
     *
     * @param restaurantId        partition key.
     * @param reservationDatetime sort key.
     * @return DTO de la reserva.
     * @throws ReservationNotFoundException si no existe.
     */
    public ReservationResponse getReservation(final String restaurantId, final String reservationDatetime) {
        return reservationRepository.findByKey(restaurantId, reservationDatetime)
                .map(ReservationResponse::fromEntity)
                .orElseThrow(() -> new ReservationNotFoundException(
                        "No existe reservacion para restaurantId=%s en %s"
                                .formatted(restaurantId, reservationDatetime)));
    }

    /**
     * Lista reservas de un restaurante en un rango de fechas.
     *
     * @param restaurantId  partition key.
     * @param startDatetime inicio del rango ISO-8601.
     * @param endDatetime   fin del rango ISO-8601.
     * @return lista de reservas (puede ser vacia).
     */
    public List<ReservationResponse> getReservationsByRestaurant(final String restaurantId,
                                                                 final String startDatetime,
                                                                 final String endDatetime) {
        LOGGER.info("Consultando reservas. restaurantId={}, rango=[{} - {}]",
                restaurantId, startDatetime, endDatetime);
        return reservationRepository.findByRestaurantBetweenDates(restaurantId, startDatetime, endDatetime)
                .stream()
                .map(ReservationResponse::fromEntity)
                .toList();
    }

    /**
     * Cancela una reserva (soft-delete: {@code status=CANCELLED}).
     *
     * @param restaurantId        partition key.
     * @param reservationDatetime sort key.
     * @return DTO con la reserva actualizada.
     * @throws ReservationNotFoundException si no existe.
     */
    public ReservationResponse cancelReservation(final String restaurantId, final String reservationDatetime) {
        final Reservation existing = reservationRepository.findByKey(restaurantId, reservationDatetime)
                .orElseThrow(() -> new ReservationNotFoundException(
                        "No existe reservacion para cancelar"));

        existing.setStatus(Reservation.Status.CANCELLED.name());
        existing.setUpdatedAt(Instant.now().toString());
        final Reservation updatedReservation = reservationRepository.save(existing);
        publish(NotificationEvent.EVENT_RESERVATION_CANCELLED, updatedReservation);
        return ReservationResponse.fromEntity(updatedReservation);
    }

    /**
     * Confirma una reserva (accion del dueno del restaurante).
     *
     * @param restaurantId        partition key.
     * @param reservationDatetime sort key.
     * @return DTO con la reserva actualizada.
     * @throws ReservationNotFoundException si no existe.
     */
    public ReservationResponse confirmReservation(final String restaurantId, final String reservationDatetime) {
        final Reservation existing = reservationRepository.findByKey(restaurantId, reservationDatetime)
                .orElseThrow(() -> new ReservationNotFoundException(
                        "No existe reservacion para confirmar"));

        existing.setStatus(Reservation.Status.CONFIRMED.name());
        existing.setUpdatedAt(Instant.now().toString());
        final Reservation confirmedReservation = reservationRepository.save(existing);
        publish(NotificationEvent.EVENT_RESERVATION_CONFIRMED, confirmedReservation);
        return ReservationResponse.fromEntity(confirmedReservation);
    }

    private void publish(final String eventType, final Reservation reservation) {
        final NotificationEvent event = new NotificationEvent(
                eventType,
                reservation.getReservationId(),
                reservation.getRestaurantId(),
                reservation.getUserId(),
                reservation.getReservationDatetime(),
                reservation.getPartySize(),
                Instant.now().toString());
        notificationPublisher.publishEvent(event);
    }
}
