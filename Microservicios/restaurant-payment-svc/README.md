# restaurant-payment-svc

Microservicio standalone (single-module Maven) para procesar pagos y reembolsos asociados a reservaciones. Persiste en DynamoDB, abstrae la pasarela de pagos y publica eventos en SNS.

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
              POST /api/v1/payments
                              |
                              v
                   PaymentController
                              |
                              v
                    PaymentService -- save --> DynamoDB (restaurant-payments, GSI reservation-index)
                       |        |
            capture/refund      publish
                       |        |
                       v        v
              PaymentGateway   SNS topic: restaurant-payments
              (simulator/real)         |
                                       v
                               (notification-svc, contabilidad, antifraude)
```

## Endpoints

| Metodo | Path | Auth | Descripcion |
|---|---|---|---|
| POST   | `/api/v1/payments` | JWT | Crear intento de pago (`PENDING`) |
| GET    | `/api/v1/payments/{paymentId}` | JWT | Obtener pago |
| GET    | `/api/v1/payments/reservation/{reservationId}` | JWT | Listar pagos de una reserva |
| POST   | `/api/v1/payments/{paymentId}/capture` | JWT | Capturar (cobrar) un `PENDING` |
| POST   | `/api/v1/payments/{paymentId}/refund` | JWT | Reembolsar un `CAPTURED` |
| GET    | `/actuator/health` | publico | Health |
| GET    | `/swagger-ui.html` | publico | OpenAPI UI |

## Estados y transiciones

```
   POST          POST /capture         POST /refund
PENDING ----------------------> CAPTURED -------------------> REFUNDED
   |  \_ pasarela rechaza
   v
FAILED  (terminal)
```

- Capturar un pago no `PENDING` -> 409 Conflict.
- Reembolsar un pago no `CAPTURED` -> 409 Conflict.
- Pasarela rechaza -> el pago queda `FAILED` con `failureReason` y se publica `PAYMENT_FAILED` en SNS.

## Pasarela

Detras de la interfaz `PaymentGatewayService`. La implementacion incluida (`SimulatedPaymentGatewayService`) esta marcada `@Profile({"dev","test","default"})` y devuelve referencias con prefijo `SIM-`. Para `prod` se debe agregar una clase real anotada con `@Profile("prod")`.

Trigger de fallo en simulador: si `amountCents == 13` lanza `PaymentGatewayException`. Util para tests del camino de error. Se desactiva con `payment.simulator.failure-trigger-enabled=false`.

## Como correr local

```bash
mvn clean verify

SPRING_PROFILES_ACTIVE=dev \
COGNITO_ISSUER_URI=https://cognito-idp.us-east-1.amazonaws.com/us-east-1_ZLhzcDygK \
SNS_TOPIC_ARN=arn:aws:sns:us-east-1:000000000000:restaurant-payments-dev \
mvn spring-boot:run
```

## Variables de entorno

| Variable | Default | Descripcion |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | `dev` | `dev` / `prod` |
| `AWS_REGION` | `us-east-1` | Region AWS |
| `DYNAMODB_TABLE_NAME` | `restaurant-payments` | Tabla destino |
| `SNS_TOPIC_ARN` | (ver `application.yml`) | Topic SNS de eventos de pago |
| `COGNITO_ISSUER_URI` | (ver `application.yml`) | Issuer Cognito |
| `PAYMENT_SIMULATOR_FAILURE_ENABLED` | `true` | Activa el trigger de fallo del simulador |

## i18n

Validacion y errores via `messages_{en,es}.properties`. `Locale` por `Accept-Language`.

## Documentacion adicional

- [GLOSSARY.md](./GLOSSARY.md)
- [docs/requests.http](./docs/requests.http)
- [docs/http-client.env.json](./docs/http-client.env.json)
- [docs/restaurant-payment-svc.postman_collection.json](./docs/restaurant-payment-svc.postman_collection.json)

## Despliegue

```bash
docker build -t restaurant-payment-svc .
```

En ECS Fargate, el task role debe tener:

- `dynamodb:GetItem`, `PutItem`, `Query` sobre la tabla y el GSI.
- `sns:Publish` sobre el topic configurado.

## Tabla DynamoDB

| Atributo | Tipo | Rol |
|---|---|---|
| `payment_id` | S | PK |
| `reservation_id` | S | GSI `reservation-index` PK |
| `user_id` | S | metadata |
| `restaurant_id` | S | metadata |
| `amount_cents` | N | monto en centavos |
| `currency` | S | ISO-4217 |
| `status` | S | PENDING/CAPTURED/FAILED/REFUNDED |
| `method` | S | CARD/WALLET/BANK_TRANSFER |
| `gateway_reference` | S | id en la pasarela |
| `failure_reason` | S | detalle si FAILED o motivo de refund |
| `created_at`, `updated_at` | S | ISO-8601 |
