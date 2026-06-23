package com.restaurant.search.dto;

import com.restaurant.search.model.Restaurant;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateRestaurantRequest(

        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 120, message = "El nombre no puede exceder 120 caracteres")
        String name,

        @Size(max = 1000, message = "La descripción no puede exceder 1000 caracteres")
        String description,

        @NotBlank(message = "La ciudad es obligatoria")
        String city,

        @NotBlank(message = "La dirección es obligatoria")
        String address,

        @NotBlank(message = "El tipo de cocina es obligatorio")
        String cuisineType,

        @Pattern(regexp = "BUDGET|MODERATE|EXPENSIVE|LUXURY",
                message = "Rango válido: BUDGET, MODERATE, EXPENSIVE, LUXURY")
        String priceRange,

        Integer seatingCapacity,

        List<String> openingHours,

        String phone,

        String email
) {

    public Restaurant toEntity(final String restaurantId, final String ownerId) {
        final Restaurant restaurant = new Restaurant();
        restaurant.setRestaurantId(restaurantId);
        restaurant.setOwnerId(ownerId);
        restaurant.setName(name);
        restaurant.setDescription(description);
        restaurant.setCity(city);
        restaurant.setAddress(address);
        restaurant.setCuisineType(cuisineType);
        restaurant.setPriceRange(priceRange);
        restaurant.setSeatingCapacity(seatingCapacity);
        restaurant.setOpeningHours(openingHours);
        restaurant.setPhone(phone);
        restaurant.setEmail(email);
        restaurant.markCreated();
        return restaurant;
    }
}
