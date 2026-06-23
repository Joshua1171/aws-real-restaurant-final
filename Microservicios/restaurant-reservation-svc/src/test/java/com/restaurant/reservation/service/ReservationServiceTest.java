package com.restaurant.reservation.service;

import com.restaurant.reservation.dto.CreateReservationRequest;
import com.restaurant.reservation.dto.NotificationEvent;
import com.restaurant.reservation.dto.ReservationResponse;
import com.restaurant.reservation.exception.ReservationNotFoundException;
import com.restaurant.reservation.model.Reservation;
import com.restaurant.reservation.repository.ReservationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests unitarios para {@link ReservationService}.
 *
 * <p>Cobertura objetivo:</p>
 * <ul>
 *   <li>Happy path: crear, consultar, listar, confirmar, cancelar.</li>
 *   <li>Error: NotFound al consultar / confirmar / cancelar.</li>
 *   <li>Side effects: el servicio publica el evento adecuado en SNS.</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private NotificationPublisherService notificationPublisher;

    @InjectMocks
    private ReservationService reservationService;

    @Test
    @DisplayName("createReservation: persiste, publica evento CREATED y devuelve DTO")
    void createReservationHappyPath() {
        final CreateReservationRequest request = new CreateReservationRequest(
                "rest-1", "2026-05-10T19:30:00Z", 4, "ventana");
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        final ReservationResponse response = reservationService.createReservation("user-1", request);

        assertThat(response.restaurantId()).isEqualTo("rest-1");
        assertThat(response.userId()).isEqualTo("user-1");
        assertThat(response.partySize()).isEqualTo(4);
        assertThat(response.status()).isEqualTo(Reservation.Status.PENDING.name());

        final ArgumentCaptor<NotificationEvent> eventCaptor = ArgumentCaptor.forClass(NotificationEvent.class);
        verify(notificationPublisher, times(1)).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().eventType()).isEqualTo(NotificationEvent.EVENT_RESERVATION_CREATED);
    }

    @Test
    @DisplayName("getReservation: lanza ReservationNotFoundException cuando no existe")
    void getReservationNotFound() {
        when(reservationRepository.findByKey("rest-1", "2026-05-10T19:30:00Z")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reservationService.getReservation("rest-1", "2026-05-10T19:30:00Z"))
                .isInstanceOf(ReservationNotFoundException.class);
    }

    @Test
    @DisplayName("getReservation: devuelve DTO cuando existe")
    void getReservationFound() {
        final Reservation reservation = Reservation.newPending(
                "rest-1", "2026-05-10T19:30:00Z", "res-1", "user-1", 2, null);
        when(reservationRepository.findByKey("rest-1", "2026-05-10T19:30:00Z"))
                .thenReturn(Optional.of(reservation));

        final ReservationResponse response = reservationService.getReservation("rest-1", "2026-05-10T19:30:00Z");
        assertThat(response.reservationId()).isEqualTo("res-1");
    }

    @Test
    @DisplayName("getReservationsByRestaurant: mapea entidades a DTOs")
    void listReservations() {
        when(reservationRepository.findByRestaurantBetweenDates("rest-1", "2026-05-10T00:00:00Z", "2026-05-10T23:59:59Z"))
                .thenReturn(List.of(
                        Reservation.newPending("rest-1", "2026-05-10T12:00:00Z", "a", "u", 2, null),
                        Reservation.newPending("rest-1", "2026-05-10T20:00:00Z", "b", "u", 3, null)));

        final List<ReservationResponse> reservations = reservationService.getReservationsByRestaurant(
                "rest-1", "2026-05-10T00:00:00Z", "2026-05-10T23:59:59Z");
        assertThat(reservations).hasSize(2);
    }

    @Test
    @DisplayName("confirmReservation: cambia status a CONFIRMED y publica evento")
    void confirmReservationHappyPath() {
        final Reservation existing = Reservation.newPending(
                "rest-1", "2026-05-10T19:30:00Z", "res-1", "user-1", 4, null);
        when(reservationRepository.findByKey("rest-1", "2026-05-10T19:30:00Z"))
                .thenReturn(Optional.of(existing));
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        final ReservationResponse response = reservationService.confirmReservation("rest-1", "2026-05-10T19:30:00Z");

        assertThat(response.status()).isEqualTo(Reservation.Status.CONFIRMED.name());
        final ArgumentCaptor<NotificationEvent> eventCaptor = ArgumentCaptor.forClass(NotificationEvent.class);
        verify(notificationPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().eventType()).isEqualTo(NotificationEvent.EVENT_RESERVATION_CONFIRMED);
    }

    @Test
    @DisplayName("cancelReservation: cambia status a CANCELLED y publica evento")
    void cancelReservationHappyPath() {
        final Reservation existing = Reservation.newPending(
                "rest-1", "2026-05-10T19:30:00Z", "res-1", "user-1", 4, null);
        when(reservationRepository.findByKey("rest-1", "2026-05-10T19:30:00Z"))
                .thenReturn(Optional.of(existing));
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        final ReservationResponse response = reservationService.cancelReservation("rest-1", "2026-05-10T19:30:00Z");

        assertThat(response.status()).isEqualTo(Reservation.Status.CANCELLED.name());
        final ArgumentCaptor<NotificationEvent> eventCaptor = ArgumentCaptor.forClass(NotificationEvent.class);
        verify(notificationPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().eventType()).isEqualTo(NotificationEvent.EVENT_RESERVATION_CANCELLED);
    }

    @Test
    @DisplayName("confirmReservation: lanza NotFound cuando no existe")
    void confirmNotFound() {
        when(reservationRepository.findByKey("x", "y")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> reservationService.confirmReservation("x", "y"))
                .isInstanceOf(ReservationNotFoundException.class);
    }
}
