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
 * Lógica de negocio del search service.
 *
 * Cache:
 *   - findById: cachea por restaurantId (alta relevancia, se consulta mucho)
 *   - findByCity: cachea por ciudad (catálogos suelen ser frecuentes)
 *   - create/update/delete: invalidan el cache
 *
 * El backend del cache puede ser Caffeine (local, por instancia) o ElastiCache Redis
 * (compartido entre tareas ECS). Se configura en application.yml.
 */
@Service
public class RestaurantService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RestaurantService.class);
    public static final String CACHE_BY_ID = "restaurantsById";
    public static final String CACHE_BY_CITY = "restaurantsByCity";

    private final RestaurantRepository restaurantRepository;

    public RestaurantService(final RestaurantRepository restaurantRepository) {
        this.restaurantRepository = restaurantRepository;
    }

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

    @Cacheable(value = CACHE_BY_ID, key = "#restaurantId")
    public RestaurantResponse getRestaurant(final String restaurantId) {
        LOGGER.debug("Consulta DynamoDB (cache miss). restaurantId={}", restaurantId);
        return restaurantRepository.findById(restaurantId)
                .map(RestaurantResponse::fromEntity)
                .orElseThrow(() -> new RestaurantNotFoundException(
                        "No existe restaurante con id=" + restaurantId));
    }

    @Cacheable(value = CACHE_BY_CITY, key = "#city.toLowerCase()")
    public List<RestaurantResponse> getRestaurantsByCity(final String city) {
        LOGGER.debug("Consulta DynamoDB GSI city-index (cache miss). city={}", city);
        return restaurantRepository.findByCity(city)
                .stream()
                .map(RestaurantResponse::fromEntity)
                .toList();
    }

    @CacheEvict(value = {CACHE_BY_ID, CACHE_BY_CITY}, allEntries = true)
    public void deleteRestaurant(final String restaurantId) {
        LOGGER.info("Eliminando restaurante. restaurantId={}", restaurantId);
        restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new RestaurantNotFoundException(
                        "No existe restaurante con id=" + restaurantId));
        restaurantRepository.deleteById(restaurantId);
    }
}
