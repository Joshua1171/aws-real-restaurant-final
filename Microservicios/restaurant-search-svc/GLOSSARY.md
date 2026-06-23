# GLOSSARY - restaurant-search-svc

## SearchApplication
**Responsabilidad.** Entry point. Habilita cache (`@EnableCaching`).
**Superficie publica.** `main(String[])`.
**Cosas que saber.** Sin logica adicional.
**Colaboraciones.** Spring Boot autoconfigura todo a partir del classpath.

---

## controller.RestaurantController
**Responsabilidad.** API REST `/api/v1/restaurants`. GET es publico, POST/DELETE requieren JWT.
**Superficie publica.** `createRestaurant`, `getRestaurant`, `searchRestaurantsByCity`, `deleteRestaurant`.
**Cosas que saber.** El `ownerId` se extrae del claim `sub` del JWT.
**Colaboraciones.** `RestaurantService`. Errores los maneja `GlobalExceptionHandler`.

---

## service.RestaurantService
**Responsabilidad.** Logica + cache (Caffeine). Crea/lee/lista/borra.
**Superficie publica.** `createRestaurant`, `getRestaurant` (`@Cacheable`), `getRestaurantsByCity` (`@Cacheable`), `deleteRestaurant` (`@CacheEvict`).
**Cosas que saber.** El delete invalida ambos caches; create tambien para no servir resultados obsoletos.
**Colaboraciones.** `RestaurantRepository`.

---

## repository.RestaurantRepository
**Responsabilidad.** Acceso a DynamoDB (Enhanced Client) con GSI `city-index`.
**Superficie publica.** `save`, `findById`, `findByCity`, `deleteById`.
**Cosas que saber.** Usar Query sobre GSI evita un Scan caro de toda la tabla.
**Colaboraciones.** `DynamoDbEnhancedClient`.

---

## model.Restaurant
**Responsabilidad.** Entidad DynamoDB. PK `restaurant_id`, GSI `city-index`.
**Superficie publica.** Getters/setters anotados, `markCreated()`, enums `Status`, `PriceRange`.
**Cosas que saber.** No usa Lombok (mismo motivo que `Reservation`).
**Colaboraciones.** Repository.

---

## dto.CreateRestaurantRequest
**Responsabilidad.** DTO de entrada. Inmutable. Provee `toEntity(...)`.
**Superficie publica.** Componentes con `@NotBlank`, `@Size`, `@Pattern` y mensajes i18n.
**Colaboraciones.** Validado en el controller; mapeado a entidad por el service.

---

## dto.RestaurantResponse
**Responsabilidad.** DTO de salida. `Serializable` para que el cache lo serialice.
**Superficie publica.** Componentes + `fromEntity`.

---

## dto.ApiError
**Responsabilidad.** Body de error (RFC 7807 simplificado).
**Superficie publica.** Factories `of(...)` y `validation(...)`.

---

## exception.RestaurantNotFoundException
**Responsabilidad.** Indica 404 desde el service. Capturada por `GlobalExceptionHandler`.

---

## exception.GlobalExceptionHandler
**Responsabilidad.** Mapea excepciones a `ApiError` con HTTP correcto. Usa `MessageSource`.

---

## config.DynamoDbConfig
**Responsabilidad.** Beans `DynamoDbClient` y `DynamoDbEnhancedClient`.

---

## config.SecurityConfig
**Responsabilidad.** Spring Security. GET publico, escritura requiere JWT.

---

## config.MessageSourceConfig
**Responsabilidad.** i18n en/es por `Accept-Language`.

---

## config.OpenApiConfig
**Responsabilidad.** Metadata OpenAPI + `bearerAuth` para Swagger UI.

---

## Database migrations
DynamoDB es NoSQL: no hay Flyway/Liquibase. La estructura de tabla y GSI esta documentada en `model.Restaurant`.
