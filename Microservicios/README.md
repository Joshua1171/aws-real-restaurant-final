# Microservicios - Restaurant Reservation System

Cinco proyectos **independientes** (single-module). Los cuatro originales se extrajeron del monorepo `restaurant-reservation-system`; el quinto (`restaurant-payment-svc`) se diseno desde cero siguiendo los mismos patrones. Cada uno tiene su propio `pom.xml` con `spring-boot-starter-parent` 3.5.5 y se compila/despliega de forma autonoma.

| Carpeta | Tipo | Puerto | Descripcion |
|---|---|---|---|
| [restaurant-reservation-svc](./restaurant-reservation-svc/) | Spring Boot REST | 8080 | CRUD de reservaciones (DynamoDB + SNS + Cognito) |
| [restaurant-search-svc](./restaurant-search-svc/) | Spring Boot REST | 8081 | Catalogo y busqueda de restaurantes (DynamoDB + Caffeine + Cognito) |
| [restaurant-notification-svc](./restaurant-notification-svc/) | Spring Boot listener | 8082 | Consumer SQS -> SES (email) + SNS (SMS) |
| [restaurant-payment-svc](./restaurant-payment-svc/) | Spring Boot REST | 8083 | Pagos y reembolsos (DynamoDB + pasarela abstraida + SNS + Cognito) |
| [lambda-stream-processor](./lambda-stream-processor/) | AWS Lambda | n/a | DynamoDB Streams -> S3 Data Lake (NDJSON) |

## Mejoras aplicadas (vs. el monorepo original)

Cada proyecto fue regenerado aplicando las skills `spring-boot-best-practices-validator` y `documentation-and-structure-validator`:

- **Single-module Maven**: `spring-boot-starter-parent` directo, sin parent multimodulo. Cada repo se puede mover a su propio Git.
- **Spring Boot 3.5.5** (estable, soporte hasta junio 2026), Java 21.
- **Perfiles dev / prod** en cada servicio (`application-dev.yml`, `application-prod.yml`).
- **Internacionalizacion** (en/es) via `MessageSource` y archivos en `src/main/resources/i18n/`. Los mensajes de validacion y errores se resuelven por `Accept-Language`.
- **Manejo de errores** con `@RestControllerAdvice` y body unificado tipo Problem Details (RFC 7807 simplificado, record `ApiError`).
- **JaCoCo 0.8.13** con umbral de cobertura por linea: 80% (servicios principales), 75% (notification), reporte HTML en `target/site/jacoco/`.
- **Logging estructurado**: `logback-spring.xml` con perfil `dev` (texto humano) y `prod` (JSON via Logstash encoder, listo para CloudWatch/ELK).
- **JavaDoc** en clases publicas, metodos publicos y record components donde aporta contexto.
- **Tests** con JUnit Jupiter 5 + Mockito 5 + AssertJ (heredados del starter). Servicios con tests unitarios + slice tests (`@WebMvcTest`).
- **OpenAPI / Swagger UI** habilitado en los servicios REST (`/swagger-ui.html`).
- **Documentacion completa** por proyecto: `README.md`, `GLOSSARY.md` (clase a clase) y carpeta `docs/` con:
  - `requests.http` (IntelliJ HTTP Client) con 3 casos por endpoint (happy / error / edge).
  - `http-client.env.json` con ambientes `local`, `dev`, `prod`.
  - `<svc>.postman_collection.json` v2.1 espejo de los `.http`.
  - `<svc>.postman_environment.json`.

## Estructura por proyecto

```
restaurant-<name>/
  pom.xml                         <- standalone, parent = spring-boot-starter-parent
  Dockerfile                      <- multi-stage build, single-module
  README.md                       <- overview, run, env vars
  GLOSSARY.md                     <- explicacion clase por clase
  src/main/java/com/restaurant/<pkg>/
    Application.java
    config/                       <- SecurityConfig, MessageSourceConfig, OpenApiConfig, etc.
    controller/                   <- REST endpoints
    dto/                          <- records de entrada/salida + ApiError
    exception/                    <- excepciones de dominio + GlobalExceptionHandler
    model/                        <- entidades DynamoDB
    repository/                   <- acceso a datos
    service/                      <- logica de negocio
    listener/ (notification-svc)  <- @SqsListener
  src/main/resources/
    application.yml
    application-dev.yml
    application-prod.yml
    logback-spring.xml
    i18n/messages_en.properties
    i18n/messages_es.properties
  src/test/java/...               <- tests unitarios + slice
  docs/
    requests.http
    http-client.env.json
    <svc>.postman_collection.json
    <svc>.postman_environment.json
```

## Como compilar todo

Cada proyecto se compila independiente:

```bash
cd restaurant-reservation-svc && mvn clean verify
cd ../restaurant-search-svc && mvn clean verify
cd ../restaurant-notification-svc && mvn clean verify
cd ../restaurant-payment-svc && mvn clean verify
cd ../lambda-stream-processor && mvn clean package
```

## Flujo end-to-end (con pagos)

```
1. Cliente -> POST /reservations           [reservation-svc]
                                          \-> SNS restaurant-notifications
                                          \-> DynamoDB Streams -> Lambda -> S3
2. Cliente -> POST /payments               [payment-svc] (PENDING)
3. Cliente -> POST /payments/{id}/capture  [payment-svc] -> CAPTURED
                                          \-> SNS restaurant-payments (PAYMENT_CAPTURED)
4. Restaurante -> PUT /reservations/.../confirm  [reservation-svc]
5. (cancelacion) Cliente -> DELETE /reservations/...  [reservation-svc]
   y Cliente -> POST /payments/{id}/refund   [payment-svc] -> REFUNDED
```

## Carpeta `_source/`

Contiene el zip original descomprimido para referencia historica. No es parte de la solucion final; puede borrarse cuando todo este verificado.
