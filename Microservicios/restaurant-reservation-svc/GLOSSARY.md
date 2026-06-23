# GLOSSARY - restaurant-reservation-svc

Una entrada por clase. Cada entrada sigue el mismo formato: Responsabilidad, Superficie publica, Cosas que saber, Colaboraciones.

---

## ReservationApplication

**Responsabilidad.** Entry point de Spring Boot. Arranca el contexto.

**Superficie publica.** `main(String[])`.

**Cosas que saber.** Sin logica adicional; toda configuracion vive en clases `@Configuration`.

**Colaboraciones.** Spring Boot autoconfigura todo a partir de las dependencias en `pom.xml`.

---

## controller.ReservationController

**Responsabilidad.** Expone la API REST `/api/v1/reservations`. Extrae el `userId` del claim `sub` del JWT, no del body.

**Superficie publica.** 5 endpoints: `POST`, `GET` (clave), `GET` (rango), `PUT confirm`, `DELETE`.

**Cosas que saber.** Anotado con `@SecurityRequirement("bearerAuth")` para que Swagger UI muestre el lock. Validacion via `@Valid` + Bean Validation.

**Colaboraciones.** Llama a `ReservationService`. El error handling lo hace `GlobalExceptionHandler`.

---

## service.ReservationService

**Responsabilidad.** Orquesta la logica de negocio: persiste en DynamoDB y dispara eventos.

**Superficie publica.** `createReservation`, `getReservation`, `getReservationsByRestaurant`, `confirmReservation`, `cancelReservation`.

**Cosas que saber.** No es transaccional (DynamoDB no soporta transacciones across items con Enhanced Client en una sola llamada). Si la publicacion en SNS falla, el item queda persistido pero sin notificacion: en produccion considerar el patron Outbox.

**Colaboraciones.** `ReservationRepository`, `NotificationPublisherService`.

---

## service.NotificationPublisherService

**Responsabilidad.** Serializa eventos a JSON y los publica en SNS, anadiendo el `MessageAttribute` `event_type` para filtering.

**Superficie publica.** `publishEvent(NotificationEvent)`.

**Cosas que saber.** Si la serializacion falla, lanza `IllegalStateException`. Cualquier otro error de SNS se relanza para que el caller decida (rollback, reintento, etc.). El topic ARN viene de `aws.sns.topic-arn`.

**Colaboraciones.** AWS SDK v2 (`SnsClient`), Jackson `ObjectMapper`.

---

## repository.ReservationRepository

**Responsabilidad.** Acceso a DynamoDB usando Enhanced Client. Encapsula el manejo de `Key` y `QueryConditional`.

**Superficie publica.** `save`, `findByKey`, `findByRestaurantBetweenDates`, `delete`.

**Cosas que saber.** `findByRestaurantBetweenDates` es un `Query` con PK + SK range -- O(log N), no Scan. El nombre de tabla es configurable via `aws.dynamodb.table-name`.

**Colaboraciones.** `DynamoDbEnhancedClient` (autoconfigurado por `DynamoDbConfig`).

---

## model.Reservation

**Responsabilidad.** Entidad DynamoDB. PK = `restaurant_id`, SK = `reservation_datetime`.

**Superficie publica.** Getters/setters anotados, factory `newPending(...)`, enum `Status`.

**Cosas que saber.** No usa Lombok porque DynamoDB Enhanced Client requiere getters/setters publicos con anotaciones explicitas (`@DynamoDbAttribute`). DynamoDB Streams esta habilitado y dispara la Lambda de archivado a S3.

**Colaboraciones.** Repository.

---

## dto.CreateReservationRequest

**Responsabilidad.** DTO de entrada al `POST`. Inmutable (record).

**Superficie publica.** Componentes del record con anotaciones `@NotBlank`, `@Min`, `@Max`, `@Size`. Mensajes en formato i18n (`{key}`).

**Cosas que saber.** El cliente NO debe enviar `userId`: se extrae del JWT.

**Colaboraciones.** Validado con `@Valid` en el controller; los mensajes los resuelve `MessageSource`.

---

## dto.ReservationResponse

**Responsabilidad.** DTO de salida (record). Provee `fromEntity(Reservation)`.

**Superficie publica.** Componentes + factory.

**Cosas que saber.** No expone propiedades sensibles; todos los campos son los del modelo.

**Colaboraciones.** Service mapea entidades a este record antes de devolverlas.

---

## dto.NotificationEvent

**Responsabilidad.** Payload publicado en SNS cuando hay cambios de estado.

**Superficie publica.** Componentes + 3 constantes para los `eventType` validos.

**Cosas que saber.** Es identico al record consumido por `restaurant-notification-svc`. Mantener sincronizado.

**Colaboraciones.** `NotificationPublisherService`.

---

## dto.ApiError

**Responsabilidad.** Body unico de error siguiendo RFC 7807 simplificado.

**Superficie publica.** `of(...)`, `validation(...)`.

**Cosas que saber.** Los `fields` solo se llenan en errores de validacion.

**Colaboraciones.** `GlobalExceptionHandler`.

---

## exception.ReservationNotFoundException

**Responsabilidad.** Senalar 404 desde el service. RuntimeException no checked.

**Superficie publica.** Constructor `(String message)`.

**Cosas que saber.** Capturada por `GlobalExceptionHandler`.

---

## exception.GlobalExceptionHandler

**Responsabilidad.** Convierte excepciones a `ApiError` con HTTP status apropiado. Maneja `ReservationNotFoundException`, `MethodArgumentNotValidException`, y un catch-all.

**Superficie publica.** 3 metodos `@ExceptionHandler`.

**Cosas que saber.** Los titulos vienen del `MessageSource` y respetan la `Locale` de la peticion (`Accept-Language`). El catch-all NO filtra el mensaje interno hacia el cliente.

**Colaboraciones.** `MessageSource`.

---

## config.DynamoDbConfig

**Responsabilidad.** Beans `DynamoDbClient` y `DynamoDbEnhancedClient`.

**Superficie publica.** Dos `@Bean`.

**Cosas que saber.** Usa `DefaultCredentialsProvider` -- en ECS Fargate se resuelve al task role.

---

## config.SnsConfig

**Responsabilidad.** Bean `SnsClient`.

**Superficie publica.** Un `@Bean`.

---

## config.SecurityConfig

**Responsabilidad.** SecurityFilterChain. Stateless. JWT Cognito requerido salvo health/swagger.

**Superficie publica.** Un `@Bean SecurityFilterChain`.

**Cosas que saber.** CSRF deshabilitado porque la API es stateless. El issuer-uri se valida contra el JWKs de Cognito automaticamente.

---

## config.MessageSourceConfig

**Responsabilidad.** Configura el `MessageSource` (i18n) y el `LocaleResolver` por `Accept-Language`.

**Superficie publica.** Dos `@Bean`.

**Cosas que saber.** Locales soportados: `en`, `es`. Default `en`.

---

## config.OpenApiConfig

**Responsabilidad.** Declara la metadata OpenAPI y el `bearerAuth` para Swagger UI.

**Superficie publica.** Anotaciones de clase, sin metodos.

---

## Database migrations

Este servicio NO usa una base de datos relacional. La persistencia es DynamoDB (NoSQL), por lo que no aplica Flyway/Liquibase.

La estructura de la tabla DynamoDB esta documentada en la entrada `model.Reservation`. Cambios de schema (nuevos atributos) son aditivos por naturaleza en DynamoDB y no requieren migraciones.

---

## Testing

Los tests unitarios estan en `src/test/java`. Para probar la API end-to-end usa `docs/requests.http` (IntelliJ) o la coleccion Postman en `docs/`.
