# restaurant-search-svc

Microservicio standalone de busqueda y gestion de restaurantes. GET es publico (catalogo abierto). Escrituras requieren JWT del dueno (Cognito).

## Stack

- Java 21
- Spring Boot 3.5.5
- Maven (single-module)
- AWS SDK v2 (DynamoDB Enhanced)
- Spring Security + OAuth2 Resource Server (Cognito)
- Spring Cache + Caffeine
- springdoc-openapi 2.6.0
- JaCoCo 0.8.13 con umbral 80% line coverage

## Endpoints

| Metodo | Path | Auth | Descripcion |
|---|---|---|---|
| POST   | `/api/v1/restaurants` | JWT | Crea un restaurante (estado `PENDING_APPROVAL`) |
| GET    | `/api/v1/restaurants/{restaurantId}` | publico | Lee por id |
| GET    | `/api/v1/restaurants/search?city=...` | publico | Busca por ciudad (cacheado) |
| DELETE | `/api/v1/restaurants/{restaurantId}` | JWT | Borra |
| GET    | `/actuator/health` | publico | Health |
| GET    | `/swagger-ui.html` | publico | OpenAPI UI |

## Como correr local

```bash
mvn clean verify

SPRING_PROFILES_ACTIVE=dev \
COGNITO_ISSUER_URI=https://cognito-idp.us-east-1.amazonaws.com/us-east-1_vWN5sL3ZG \
mvn spring-boot:run
```

`http://localhost:8081/actuator/health` debe responder 200.

## Variables de entorno

| Variable | Default | Descripcion |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | `dev` | `dev` o `prod` |
| `AWS_REGION` | `us-east-1` | Region AWS |
| `DYNAMODB_TABLE_NAME` | `restaurant-restaurants` | Tabla |
| `COGNITO_ISSUER_URI` | (ver `application.yml`) | Issuer Cognito |

## Cache

Backend Caffeine local. En `dev` mantiene 1000 entradas con TTL 5 min. En `prod` 5000 entradas con TTL 10 min.

## i18n

Mensajes en `src/main/resources/i18n/messages_{en,es}.properties`. Locale por `Accept-Language`.

## Documentacion adicional

- [GLOSSARY.md](./GLOSSARY.md)
- [docs/requests.http](./docs/requests.http)
- [docs/http-client.env.json](./docs/http-client.env.json)
- [docs/restaurant-search-svc.postman_collection.json](./docs/restaurant-search-svc.postman_collection.json)
