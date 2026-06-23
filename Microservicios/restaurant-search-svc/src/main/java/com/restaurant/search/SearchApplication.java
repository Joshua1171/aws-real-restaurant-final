package com.restaurant.search;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

/**
 * Microservicio standalone de busqueda y gestion de restaurantes.
 *
 * <p>Responsabilidades:</p>
 * <ul>
 *   <li>CRUD de restaurantes (gestion por parte de los duenos).</li>
 *   <li>Busqueda por ciudad, cocina, rango de precio.</li>
 *   <li>Cache de resultados frecuentes (reduce lecturas a DynamoDB).</li>
 * </ul>
 *
 * <p>Tabla DynamoDB: {@code restaurant-restaurants}.<br>
 * Cognito User Pool: {@code restaurant-owners-pool}.<br>
 * Puerto por defecto: 8081.</p>
 *
 * @author Joshua
 * @since 1.0.0
 */
@SpringBootApplication
@EnableCaching
public class SearchApplication {

    /**
     * Arranca el contexto de Spring Boot.
     *
     * @param args argumentos de linea de comandos.
     */
    public static void main(final String[] args) {
        SpringApplication.run(SearchApplication.class, args);
    }
}
