package com.restaurant.search.model;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondaryPartitionKey;

import java.time.Instant;
import java.util.List;

/**
 * Entidad Restaurant en DynamoDB.
 *
 * <p>Tabla: {@code restaurant-restaurants}</p>
 * <ul>
 *   <li><b>PK</b>: {@code restaurant_id} (String).</li>
 *   <li><b>GSI</b> {@code city-index}: {@code city} -- permite buscar sin Scan.</li>
 * </ul>
 *
 * @author Joshua
 * @since 1.0.0
 */
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

    /** Constructor por defecto requerido por DynamoDB Enhanced. */
    public Restaurant() {
        // requerido por DynamoDB Enhanced Client
    }

    @DynamoDbPartitionKey
    @DynamoDbAttribute("restaurant_id")
    public String getRestaurantId() {
        return restaurantId;
    }

    public void setRestaurantId(final String restaurantId) {
        this.restaurantId = restaurantId;
    }

    @DynamoDbAttribute("owner_id")
    public String getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(final String ownerId) {
        this.ownerId = ownerId;
    }

    @DynamoDbAttribute("name")
    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    @DynamoDbAttribute("description")
    public String getDescription() {
        return description;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    @DynamoDbSecondaryPartitionKey(indexNames = "city-index")
    @DynamoDbAttribute("city")
    public String getCity() {
        return city;
    }

    public void setCity(final String city) {
        this.city = city;
    }

    @DynamoDbAttribute("address")
    public String getAddress() {
        return address;
    }

    public void setAddress(final String address) {
        this.address = address;
    }

    @DynamoDbAttribute("cuisine_type")
    public String getCuisineType() {
        return cuisineType;
    }

    public void setCuisineType(final String cuisineType) {
        this.cuisineType = cuisineType;
    }

    @DynamoDbAttribute("price_range")
    public String getPriceRange() {
        return priceRange;
    }

    public void setPriceRange(final String priceRange) {
        this.priceRange = priceRange;
    }

    @DynamoDbAttribute("rating")
    public Double getRating() {
        return rating;
    }

    public void setRating(final Double rating) {
        this.rating = rating;
    }

    @DynamoDbAttribute("review_count")
    public Integer getReviewCount() {
        return reviewCount;
    }

    public void setReviewCount(final Integer reviewCount) {
        this.reviewCount = reviewCount;
    }

    @DynamoDbAttribute("opening_hours")
    public List<String> getOpeningHours() {
        return openingHours;
    }

    public void setOpeningHours(final List<String> openingHours) {
        this.openingHours = openingHours;
    }

    @DynamoDbAttribute("seating_capacity")
    public Integer getSeatingCapacity() {
        return seatingCapacity;
    }

    public void setSeatingCapacity(final Integer seatingCapacity) {
        this.seatingCapacity = seatingCapacity;
    }

    @DynamoDbAttribute("phone")
    public String getPhone() {
        return phone;
    }

    public void setPhone(final String phone) {
        this.phone = phone;
    }

    @DynamoDbAttribute("email")
    public String getEmail() {
        return email;
    }

    public void setEmail(final String email) {
        this.email = email;
    }

    @DynamoDbAttribute("status")
    public String getStatus() {
        return status;
    }

    public void setStatus(final String status) {
        this.status = status;
    }

    @DynamoDbAttribute("created_at")
    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(final String createdAt) {
        this.createdAt = createdAt;
    }

    @DynamoDbAttribute("updated_at")
    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(final String updatedAt) {
        this.updatedAt = updatedAt;
    }

    /** Estados validos. */
    public enum Status { ACTIVE, PENDING_APPROVAL, SUSPENDED, CLOSED }

    /** Rango de precio. */
    public enum PriceRange { BUDGET, MODERATE, EXPENSIVE, LUXURY }

    /**
     * Inicializa timestamps y estado por defecto al crear.
     */
    public void markCreated() {
        final String now = Instant.now().toString();
        this.createdAt = now;
        this.updatedAt = now;
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
