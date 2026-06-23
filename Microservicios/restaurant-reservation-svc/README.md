# restaurant-reservation-svc

Microservicio standalone (single-module Maven) que gestiona reservaciones de restaurantes. Persiste en DynamoDB, publica eventos en SNS y autentica usuarios con JWT de Cognito.

## Stack

- Java 21
- Spring Boot 3.5.5
- Maven (single-module, sin parent multimodulo)
- AWS SDK v2 (DynamoDB Enhanced + SNS)
- Spring Security + OAuth2 Resource Server (Cognito)
- springdoc-openapi 2.6.0
- JaCoCo 0.8.13 con umbral 80% line coverage

## Arquitectura

```
            JWT (Cognito)
                |
   POST /api/v1/reservations
                |
                v
   ReservationController
                |
                v
   ReservationService -- save -->  DynamoDB (restaurant-reservations)
                |                          |
                |                          v
                |                    DynamoDB Streams --> Lambda --> S3 (Data Lake)
                |
                +-- publish ----> SNS topic: restaurant-notifications
                                          |
                                          v
                                  SQS queue (fan-out)
                                          |
                                          v
                                 restaurant-notification-svc
```

## Endpoints

| Metodo | Path | Auth | Descripcion |
|---|---|---|---|
| POST   | `/api/v1/reservations` | JWT | Crea una reserva en estado `PENDING` |
| GET    | `/api/v1/reservations/{restaurantId}/{reservationDatetime}` | JWT | Lee una reserva por su clave compuesta |
| GET    | `/api/v1/reservations/restaurant/{restaurantId}?startDatetime=...&endDatetime=...` | JWT | Lista reservas de un restaurante en un rango |
| PUT    | `/api/v1/reservations/{restaurantId}/{reservationDatetime}/confirm` | JWT | Confirma la reserva |
| DELETE | `/api/v1/reservations/{restaurantId}/{reservationDatetime}` | JWT | Cancela (soft-delete a status=CANCELLED) |
| GET    | `/actuator/health` | publico | Health check para ALB/ECS |
| GET    | `/swagger-ui.html` | publico | OpenAPI UI |

## Como correr local

Requisitos: Java 21, Maven 3.9+, credenciales AWS o LocalStack.

```bash
# 1. Compilar y testear
mvn clean verify

# 2. Arrancar (perfil dev)
SPRING_PROFILES_ACTIVE=dev \
COGNITO_ISSUER_URI=https://cognito-idp.us-east-1.amazonaws.com/us-east-1_ZLhzcDygK \
SNS_TOPIC_ARN=arn:aws:sns:us-east-1:000000000000:restaurant-notifications-dev \
mvn spring-boot:run

# 3. Verificar
curl http://localhost:8080/actuator/health
```

## Variables de entorno

| Variable | Default | Descripcion |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | `dev` | Perfil activo (`dev` / `prod`) |
| `AWS_REGION` | `us-east-1` | Region AWS |
| `DYNAMODB_TABLE_NAME` | `restaurant-reservations` | Tabla destino |
| `SNS_TOPIC_ARN` | (ver `application.yml`) | ARN del topic SNS |
| `COGNITO_ISSUER_URI` | (ver `application.yml`) | Issuer URL de Cognito |

## Perfiles

- `dev`: log textual, nivel `DEBUG` para `com.restaurant`. Tablas `*-dev`.
- `prod`: log JSON estructurado (Logstash encoder), nivel `WARN`/`INFO`. Health sin detalles.

## Internacionalizacion

Mensajes de validacion y errores se resuelven via `MessageSource` con archivos:

- `src/main/resources/i18n/messages_en.properties`
- `src/main/resources/i18n/messages_es.properties`

El `Locale` se elige por la cabecera `Accept-Language`.

## Documentacion adicional

- [GLOSSARY.md](./GLOSSARY.md) - explicacion clase por clase.
- [docs/requests.http](./docs/requests.http) - colleccion HTTP para IntelliJ.
- [docs/http-client.env.json](./docs/http-client.env.json) - environments IntelliJ.
- [docs/restaurant-reservation-svc.postman_collection.json](./docs/restaurant-reservation-svc.postman_collection.json) - coleccion Postman v2.1.

## Testing

```bash
mvn test            # Tests unitarios + slice
mvn verify          # Tests + JaCoCo (falla si line coverage < 80%)
```

Reporte de cobertura HTML: `target/site/jacoco/index.html`.

## Despliegue

`docker build -t restaurant-reservation-svc .`

En ECS Fargate, el task role `restaurant-ecs-task-role` debe tener:
- `AmazonDynamoDBFullAccess` (o policy minima sobre la tabla).
- `sns:Publish` sobre el topic configurado.
