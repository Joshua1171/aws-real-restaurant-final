package com.restaurant.search.service;

import com.restaurant.search.dto.CreateRestaurantRequest;
import com.restaurant.search.dto.RestaurantResponse;
import com.restaurant.search.exception.RestaurantNotFoundException;
import com.restaurant.search.model.Restaurant;
import com.restaurant.search.repository.RestaurantRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
 * Tests unitarios para {@link RestaurantService}.
 *
 * <p>Cobertura:</p>
 * <ul>
 *   <li>Happy path crear/leer/listar/borrar.</li>
 *   <li>Errores: NotFound al leer y borrar.</li>
 *   <li>Side effect: borrado verifica existencia antes de delete.</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class RestaurantServiceTest {

    @Mock
    private RestaurantRepository restaurantRepository;

    @InjectMocks
    private RestaurantService restaurantService;

    @Test
    @DisplayName("createRestaurant: persiste y devuelve DTO con campos del request")
    void createRestaurantHappyPath() {
        final CreateRestaurantRequest request = new CreateRestaurantRequest(
                "La Bodeguita", "Cocina mexicana", "Toluca", "Calle 5",
                "MEXICAN", "MODERATE", 40, List.of("L-V 13:00-22:00"), "+5217221234567", "info@bodeguita.mx");
        when(restaurantRepository.save(any(Restaurant.class))).thenAnswer(invocation -> invocation.getArgument(0));

        final RestaurantResponse response = restaurantService.createRestaurant("owner-1", request);

        assertThat(response.name()).isEqualTo("La Bodeguita");
        assertThat(response.ownerId()).isEqualTo("owner-1");
        assertThat(response.status()).isEqualTo(Restaurant.Status.PENDING_APPROVAL.name());
    }

    @Test
    @DisplayName("getRestaurant: existe -> DTO")
    void getRestaurantFound() {
        final Restaurant restaurant = new Restaurant();
        restaurant.setRestaurantId("rest-1");
        restaurant.setName("X");
        restaurant.setCity("Toluca");
        restaurant.markCreated();
        when(restaurantRepository.findById("rest-1")).thenReturn(Optional.of(restaurant));

        final RestaurantResponse response = restaurantService.getRestaurant("rest-1");
        assertThat(response.restaurantId()).isEqualTo("rest-1");
    }

    @Test
    @DisplayName("getRestaurant: no existe -> RestaurantNotFoundException")
    void getRestaurantNotFound() {
        when(restaurantRepository.findById("rest-x")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> restaurantService.getRestaurant("rest-x"))
                .isInstanceOf(RestaurantNotFoundException.class);
    }

    @Test
    @DisplayName("getRestaurantsByCity: mapea entidades a DTOs")
    void listByCity() {
        final Restaurant a = new Restaurant();
        a.setRestaurantId("a");
        a.setCity("Toluca");
        a.markCreated();
        when(restaurantRepository.findByCity("Toluca")).thenReturn(List.of(a));

        final List<RestaurantResponse> restaurants = restaurantService.getRestaurantsByCity("Toluca");
        assertThat(restaurants).hasSize(1);
    }

    @Test
    @DisplayName("deleteRestaurant: existe -> verifica y borra")
    void deleteRestaurantHappyPath() {
        final Restaurant restaurant = new Restaurant();
        restaurant.setRestaurantId("rest-1");
        restaurant.setCity("Toluca");
        when(restaurantRepository.findById("rest-1")).thenReturn(Optional.of(restaurant));

        restaurantService.deleteRestaurant("rest-1");
        verify(restaurantRepository, times(1)).deleteById("rest-1");
    }

    @Test
    @DisplayName("deleteRestaurant: no existe -> NotFound y no llama a deleteById")
    void deleteRestaurantNotFound() {
        when(restaurantRepository.findById("rest-x")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> restaurantService.deleteRestaurant("rest-x"))
                .isInstanceOf(RestaurantNotFoundException.class);
        verify(restaurantRepository, times(0)).deleteById(any());
    }
}
