package com.restaurant.search.dto;

import com.restaurant.search.model.Restaurant;

import java.io.Serializable;
import java.util.List;

/**
 * DTO de respuesta para Restaurant.
 *
 * <p>Implementa {@link Serializable} para que pueda ser cacheable con Caffeine
 * o Redis sin custom serializers.</p>
 *
 * @author Joshua
 * @since 1.0.0
 */
public record RestaurantResponse(
        String restaurantId,
        String ownerId,
        String name,
        String description,
        String city,
        String address,
        String cuisineType,
        String priceRange,
        Double rating,
        Integer reviewCount,
        List<String> openingHours,
        Integer seatingCapacity,
        String phone,
        String email,
        String status,
        String createdAt,
        String updatedAt
) implements Serializable {

    /**
     * @param restaurant entidad fuente.
     * @return DTO listo para serializar.
     */
    public static RestaurantResponse fromEntity(final Restaurant restaurant) {
        return new RestaurantResponse(
                restaurant.getRestaurantId(),
                restaurant.getOwnerId(),
                restaurant.getName(),
                restaurant.getDescription(),
                restaurant.getCity(),
                restaurant.getAddress(),
                restaurant.getCuisineType(),
                restaurant.getPriceRange(),
                restaurant.getRating(),
                restaurant.getReviewCount(),
                restaurant.getOpeningHours(),
                restaurant.getSeatingCapacity(),
                restaurant.getPhone(),
                restaurant.getEmail(),
                restaurant.getStatus(),
                restaurant.getCreatedAt(),
                restaurant.getUpdatedAt());
    }
}
