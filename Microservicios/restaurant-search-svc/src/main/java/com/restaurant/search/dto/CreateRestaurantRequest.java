package com.restaurant.search.dto;

import com.restaurant.search.model.Restaurant;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * DTO de entrada para crear un restaurante.
 *
 * <p>Los mensajes de error referencian claves del {@code MessageSource} via {@code {key}}.</p>
 *
 * @author Joshua
 * @since 1.0.0
 */
public record CreateRestaurantRequest(

        @NotBlank(message = "{restaurant.name.required}")
        @Size(max = 120, message = "{restaurant.name.size}")
        String name,

        @Size(max = 1000, message = "{restaurant.description.size}")
        String description,

        @NotBlank(message = "{restaurant.city.required}")
        String city,

        @NotBlank(message = "{restaurant.address.required}")
        String address,

        @NotBlank(message = "{restaurant.cuisine.required}")
        String cuisineType,

        @Pattern(regexp = "BUDGET|MODERATE|EXPENSIVE|LUXURY", message = "{restaurant.priceRange.invalid}")
        String priceRange,

        Integer seatingCapacity,
        List<String> openingHours,
        String phone,
        String email
) {

    /**
     * Convierte este DTO en una entidad {@link Restaurant} lista para persistir.
     *
     * @param restaurantId id generado por el service (UUID).
     * @param ownerId      id del dueno autenticado (claim "sub" del JWT).
     * @return entidad inicializada con timestamps y estado por defecto.
     */
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
