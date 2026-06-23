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
 * Orquesta la lógica de negocio de reservaciones.
 *
 * Flujo "crear reservación":
 *   1. Genera un reservationId único (UUID)
 *   2. Construye la entidad con status=PENDING
 *   3. Persiste en DynamoDB (el Stream notificará a Lambda → S3)
 *   4. Publica evento RESERVATION_CREATED en SNS
 *   5. Retorna el DTO al controller
 */
@Service
public class ReservationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReservationService.class);

    private final ReservationRepository reservationRepository;
    private final NotificationPublisherService notificationPublisher;

    public ReservationService(final ReservationRepository reservationRepository,
                              final NotificationPublisherService notificationPublisher) {
        this.reservationRepository = reservationRepository;
        this.notificationPublisher = notificationPublisher;
    }

    public ReservationResponse createReservation(final String authenticatedUserId,
                                                 final CreateReservationRequest request) {
        final String newReservationId = UUID.randomUUID().toString();
        LOGGER.info("Creando reservación. userId={}, restaurantId={}, datetime={}",
                authenticatedUserId, request.restaurantId(), request.reservationDatetime());

        final Reservation reservation = Reservation.newPending(
                request.restaurantId(),
                request.reservationDatetime(),
                newReservationId,
                authenticatedUserId,
                request.partySize(),
                request.specialRequests());

        final Reservation savedReservation = reservationRepository.save(reservation);

        final NotificationEvent createdEvent = new NotificationEvent(
                NotificationEvent.EVENT_RESERVATION_CREATED,
                savedReservation.getReservationId(),
                savedReservation.getRestaurantId(),
                savedReservation.getUserId(),
                savedReservation.getReservationDatetime(),
                savedReservation.getPartySize(),
                Instant.now().toString());
        notificationPublisher.publishEvent(createdEvent);

        return ReservationResponse.fromEntity(savedReservation);
    }

    public ReservationResponse getReservation(final String restaurantId, final String reservationDatetime) {
        return reservationRepository.findByKey(restaurantId, reservationDatetime)
                .map(ReservationResponse::fromEntity)
                .orElseThrow(() -> new ReservationNotFoundException(
                        "No existe reservación para restaurantId=%s en %s".formatted(restaurantId, reservationDatetime)));
    }

    public List<ReservationResponse> getReservationsByRestaurant(final String restaurantId,
                                                                 final String startDatetime,
                                                                 final String endDatetime) {
        LOGGER.info("Consultando reservaciones. restaurantId={}, rango=[{} - {}]",
                restaurantId, startDatetime, endDatetime);
        return reservationRepository.findByRestaurantBetweenDates(restaurantId, startDatetime, endDatetime)
                .stream()
                .map(ReservationResponse::fromEntity)
                .toList();
    }

    public ReservationResponse cancelReservation(final String restaurantId, final String reservationDatetime) {
        final Reservation existing = reservationRepository.findByKey(restaurantId, reservationDatetime)
                .orElseThrow(() -> new ReservationNotFoundException(
                        "No existe reservación para cancelar"));

        existing.setStatus(Reservation.Status.CANCELLED.name());
        existing.setUpdatedAt(Instant.now().toString());
        final Reservation updatedReservation = reservationRepository.save(existing);

        final NotificationEvent cancelledEvent = new NotificationEvent(
                NotificationEvent.EVENT_RESERVATION_CANCELLED,
                updatedReservation.getReservationId(),
                updatedReservation.getRestaurantId(),
                updatedReservation.getUserId(),
                updatedReservation.getReservationDatetime(),
                updatedReservation.getPartySize(),
                Instant.now().toString());
        notificationPublisher.publishEvent(cancelledEvent);

        return ReservationResponse.fromEntity(updatedReservation);
    }

    public ReservationResponse confirmReservation(final String restaurantId, final String reservationDatetime) {
        final Reservation existing = reservationRepository.findByKey(restaurantId, reservationDatetime)
                .orElseThrow(() -> new ReservationNotFoundException(
                        "No existe reservación para confirmar"));

        existing.setStatus(Reservation.Status.CONFIRMED.name());
        existing.setUpdatedAt(Instant.now().toString());
        final Reservation confirmedReservation = reservationRepository.save(existing);

        final NotificationEvent confirmedEvent = new NotificationEvent(
                NotificationEvent.EVENT_RESERVATION_CONFIRMED,
                confirmedReservation.getReservationId(),
                confirmedReservation.getRestaurantId(),
                confirmedReservation.getUserId(),
                confirmedReservation.getReservationDatetime(),
                confirmedReservation.getPartySize(),
                Instant.now().toString());
        notificationPublisher.publishEvent(confirmedEvent);

        return ReservationResponse.fromEntity(confirmedReservation);
    }
}
