package com.restaurant.search;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

/**
 * Microservicio de búsqueda de restaurantes.
 *
 * Responsabilidades:
 * - CRUD de restaurantes (gestión por parte de los dueños)
 * - Búsqueda por ciudad, cocina, rango de precio
 * - Caché de resultados frecuentes (reduce lectura a DynamoDB)
 *
 * Tabla DynamoDB: restaurant-restaurants (PK: restaurant_id)
 * Autenticación Cognito: restaurant-owners-pool
 * Puerto: 8081
 */
@SpringBootApplication
@EnableCaching
public class SearchApplication {

    public static void main(final String[] args) {
        SpringApplication.run(SearchApplication.class, args);
    }
}
