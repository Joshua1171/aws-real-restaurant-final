package com.restaurant.search.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondaryPartitionKey;

import java.time.Instant;
import java.util.List;

/**
 * Entidad Restaurant en DynamoDB.
 *
 * Tabla: restaurant-restaurants
 *   - Partition Key: restaurant_id (String)
 *   - GSI: city-index (PK: city) — permite buscar por ciudad sin Scan
 *
 * Patrones de acceso:
 *   - "Dame el restaurante X" → GetItem(restaurantId)
 *   - "Restaurantes en Toluca" → Query sobre GSI city-index
 */
@Getter
@Setter
@NoArgsConstructor
@DynamoDbBean
public class Restaurant {

    private String restaurantId;
    private String ownerId;
    private String name;
    private String description;
    private String city;
    private String address;
    private String cuisineType;
    private String priceRange;
    private Double rating;
    private Integer reviewCount;
    private List<String> openingHours;
    private Integer seatingCapacity;
    private String phone;
    private String email;
    private String status;
    private String createdAt;
    private String updatedAt;

    @DynamoDbPartitionKey
    @DynamoDbAttribute("restaurant_id")
    public String getRestaurantId() {
        return restaurantId;
    }

    @DynamoDbAttribute("owner_id")
    public String getOwnerId() {
        return ownerId;
    }

    @DynamoDbAttribute("name")
    public String getName() {
        return name;
    }

    @DynamoDbAttribute("description")
    public String getDescription() {
        return description;
    }

    @DynamoDbSecondaryPartitionKey(indexNames = "city-index")
    @DynamoDbAttribute("city")
    public String getCity() {
        return city;
    }

    @DynamoDbAttribute("address")
    public String getAddress() {
        return address;
    }

    @DynamoDbAttribute("cuisine_type")
    public String getCuisineType() {
        return cuisineType;
    }

    @DynamoDbAttribute("price_range")
    public String getPriceRange() {
        return priceRange;
    }

    @DynamoDbAttribute("rating")
    public Double getRating() {
        return rating;
    }

    @DynamoDbAttribute("review_count")
    public Integer getReviewCount() {
        return reviewCount;
    }

    @DynamoDbAttribute("opening_hours")
    public List<String> getOpeningHours() {
        return openingHours;
    }

    @DynamoDbAttribute("seating_capacity")
    public Integer getSeatingCapacity() {
        return seatingCapacity;
    }

    @DynamoDbAttribute("phone")
    public String getPhone() {
        return phone;
    }

    @DynamoDbAttribute("email")
    public String getEmail() {
        return email;
    }

    @DynamoDbAttribute("status")
    public String getStatus() {
        return status;
    }

    @DynamoDbAttribute("created_at")
    public String getCreatedAt() {
        return createdAt;
    }

    @DynamoDbAttribute("updated_at")
    public String getUpdatedAt() {
        return updatedAt;
    }

    public enum Status {
        ACTIVE,
        PENDING_APPROVAL,
        SUSPENDED,
        CLOSED
    }

    public enum PriceRange {
        BUDGET,
        MODERATE,
        EXPENSIVE,
        LUXURY
    }

    public void markCreated() {
        final String timestampNow = Instant.now().toString();
        this.createdAt = timestampNow;
        this.updatedAt = timestampNow;
        if (this.status == null) {
            this.status = Status.PENDING_APPROVAL.name();
        }
        if (this.rating == null) {
            this.rating = 0.0;
        }
        if (this.reviewCount == null) {
            this.reviewCount = 0;
        }
    }
}
