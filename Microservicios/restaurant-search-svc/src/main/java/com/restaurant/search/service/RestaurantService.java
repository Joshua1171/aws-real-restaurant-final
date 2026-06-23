package com.restaurant.search.service;

import com.restaurant.search.dto.CreateRestaurantRequest;
import com.restaurant.search.dto.RestaurantResponse;
import com.restaurant.search.exception.RestaurantNotFoundException;
import com.restaurant.search.model.Restaurant;
import com.restaurant.search.repository.RestaurantRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * Logica de negocio del search service.
 *
 * <p>Cache:</p>
 * <ul>
 *   <li>{@link #getRestaurant(String)}: cachea por restaurantId.</li>
 *   <li>{@link #getRestaurantsByCity(String)}: cachea por ciudad (lowercase).</li>
 *   <li>{@code create}/{@code delete}: invalidan ambos caches.</li>
 * </ul>
 *
 * <p>Backend de cache: Caffeine local (configurado en {@code application-*.yml}).</p>
 *
 * @author Joshua
 * @since 1.0.0
 */
@Service
public class RestaurantService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RestaurantService.class);

    /** Nombre del cache por restaurantId. */
    public static final String CACHE_BY_ID = "restaurantsById";
    /** Nombre del cache por ciudad. */
    public static final String CACHE_BY_CITY = "restaurantsByCity";

    private final RestaurantRepository restaurantRepository;

    /**
     * @param restaurantRepository repo DynamoDB.
     */
    public RestaurantService(final RestaurantRepository restaurantRepository) {
        this.restaurantRepository = restaurantRepository;
    }

    /**
     * Crea un restaurante e invalida los caches.
     *
     * @param ownerId id del dueno (claim "sub" del JWT).
     * @param request payload validado.
     * @return DTO del restaurante creado.
     */
    @CacheEvict(value = {CACHE_BY_ID, CACHE_BY_CITY}, allEntries = true)
    public RestaurantResponse createRestaurant(final String ownerId,
                                               final CreateRestaurantRequest request) {
        final String newRestaurantId = UUID.randomUUID().toString();
        LOGGER.info("Creando restaurante. ownerId={}, name={}, city={}",
                ownerId, request.name(), request.city());

        final Restaurant restaurantEntity = request.toEntity(newRestaurantId, ownerId);
        final Restaurant savedRestaurant = restaurantRepository.save(restaurantEntity);
        return RestaurantResponse.fromEntity(savedRestaurant);
    }

    /**
     * @param restaurantId PK.
     * @return DTO del restaurante.
     * @throws RestaurantNotFoundException si no existe.
     */
    @Cacheable(value = CACHE_BY_ID, key = "#restaurantId")
    public RestaurantResponse getRestaurant(final String restaurantId) {
        LOGGER.debug("Consulta DynamoDB (cache miss). restaurantId={}", restaurantId);
        return restaurantRepository.findById(restaurantId)
                .map(RestaurantResponse::fromEntity)
                .orElseThrow(() -> new RestaurantNotFoundException(
                        "No existe restaurante con id=" + restaurantId));
    }

    /**
     * @param city ciudad (no se normaliza el casing en el query, pero el cache key si).
     * @return restaurantes (puede ser vacia).
     */
    @Cacheable(value = CACHE_BY_CITY, key = "#city.toLowerCase()")
    public List<RestaurantResponse> getRestaurantsByCity(final String city) {
        LOGGER.debug("Consulta DynamoDB GSI city-index (cache miss). city={}", city);
        return restaurantRepository.findByCity(city)
                .stream()
                .map(RestaurantResponse::fromEntity)
                .toList();
    }

    /**
     * Borra un restaurante e invalida los caches.
     *
     * @param restaurantId PK.
     * @throws RestaurantNotFoundException si no existe.
     */
    @CacheEvict(value = {CACHE_BY_ID, CACHE_BY_CITY}, allEntries = true)
    public void deleteRestaurant(final String restaurantId) {
        LOGGER.info("Eliminando restaurante. restaurantId={}", restaurantId);
        restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new RestaurantNotFoundException(
                        "No existe restaurante con id=" + restaurantId));
        restaurantRepository.deleteById(restaurantId);
    }
}
