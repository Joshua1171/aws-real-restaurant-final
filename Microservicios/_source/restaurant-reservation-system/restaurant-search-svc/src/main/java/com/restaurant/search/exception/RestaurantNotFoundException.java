package com.restaurant.search.exception;

public class RestaurantNotFoundException extends RuntimeException {
    public RestaurantNotFoundException(final String message) {
        super(message);
    }
}
