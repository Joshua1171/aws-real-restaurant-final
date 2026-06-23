package com.restaurant.search.dto;

import com.restaurant.search.model.Restaurant;

import java.io.Serializable;
import java.util.List;

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
