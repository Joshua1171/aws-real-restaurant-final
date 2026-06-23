package com.restaurant.search.repository;

import com.restaurant.search.model.Restaurant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbIndex;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Acceso a datos para Restaurant. Usa el GSI {@code city-index} para busquedas por ciudad
 * sin recurrir a {@code Scan}.
 *
 * @author Joshua
 * @since 1.0.0
 */
@Repository
public class RestaurantRepository {

    private static final String CITY_INDEX_NAME = "city-index";

    private final DynamoDbTable<Restaurant> restaurantTable;
    private final DynamoDbIndex<Restaurant> cityIndex;

    /**
     * @param enhancedClient cliente DynamoDB Enhanced.
     * @param tableName      nombre logico de la tabla.
     */
    public RestaurantRepository(final DynamoDbEnhancedClient enhancedClient,
                                @Value("${aws.dynamodb.table-name:restaurant-restaurants}") final String tableName) {
        this.restaurantTable = enhancedClient.table(tableName, TableSchema.fromBean(Restaurant.class));
        this.cityIndex = restaurantTable.index(CITY_INDEX_NAME);
    }

    /**
     * @param restaurant entidad a guardar.
     * @return la misma entidad.
     */
    public Restaurant save(final Restaurant restaurant) {
        restaurantTable.putItem(restaurant);
        return restaurant;
    }

    /**
     * @param restaurantId PK.
     * @return entidad si existe.
     */
    public Optional<Restaurant> findById(final String restaurantId) {
        final Key primaryKey = Key.builder().partitionValue(restaurantId).build();
        return Optional.ofNullable(restaurantTable.getItem(primaryKey));
    }

    /**
     * Query sobre el GSI {@code city-index}.
     *
     * @param city ciudad exacta.
     * @return restaurantes en la ciudad (puede ser vacia).
     */
    public List<Restaurant> findByCity(final String city) {
        final QueryConditional queryByCity = QueryConditional.keyEqualTo(
                Key.builder().partitionValue(city).build());

        return cityIndex.query(queryByCity)
                .stream()
                .flatMap(page -> page.items().stream())
                .collect(Collectors.toList());
    }

    /**
     * @param restaurantId PK del item a borrar.
     */
    public void deleteById(final String restaurantId) {
        final Key primaryKey = Key.builder().partitionValue(restaurantId).build();
        restaurantTable.deleteItem(primaryKey);
    }
}
