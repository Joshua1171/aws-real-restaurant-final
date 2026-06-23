# restaurant-notification-svc

Microservicio standalone consumidor de SQS. Recibe eventos de cambios en reservaciones (publicados por `restaurant-reservation-svc` via SNS) y envia notificaciones por email (SES) y SMS (SNS).

## Stack

- Java 21
- Spring Boot 3.5.5
- Maven (single-module)
- Spring Cloud AWS 3.2.1 (SQS Listener)
- AWS SDK v2 (SES, SNS)
- JaCoCo 0.8.13 con umbral 75% line coverage

## Arquitectura

```
reservation-svc --publish--> SNS (restaurant-notifications)
                                |
                                v
                       SQS (restaurant-notifications-queue)
                                |
                                v
                  notification-svc (@SqsListener)
                       |                |
                       v                v
                     SES (email)    SNS SMS (telefono)
```

Si el procesamiento falla, SQS reentregará tras el visibility timeout. Tras N reintentos pasa a la DLQ.

## Endpoints HTTP

Este servicio no expone API de negocio. Solo:

| Metodo | Path | Descripcion |
|---|---|---|
| GET | `/api/v1/notifications/status` | Status JSON {service, status, timestamp} |
| GET | `/actuator/health` | Health para ALB/ECS |

## Como correr local

```bash
mvn clean verify

SPRING_PROFILES_ACTIVE=dev \
AWS_REGION=us-east-1 \
SQS_QUEUE_NAME=restaurant-notifications-queue-dev \
SES_SENDER_EMAIL=verified@your-domain.com \
DEFAULT_RECIPIENT_EMAIL=your-verified@inbox.com \
mvn spring-boot:run
```

## Variables de entorno

| Variable | Default | Descripcion |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | `dev` | `dev` / `prod` |
| `AWS_REGION` | `us-east-1` | Region AWS |
| `SQS_QUEUE_NAME` | `restaurant-notifications-queue` | Cola SQS a consumir |
| `SES_SENDER_EMAIL` | `no-reply@restaurant-lab.com` | Remitente (verificado en SES) |
| `DEFAULT_RECIPIENT_EMAIL` | `lab-test@example.com` | Placeholder de prueba |

## i18n

Mensajes de email/SMS en `i18n/messages_{en,es}.properties`. El `Locale` se elige por la cabecera `Accept-Language` (cuando aplica) o el default del JVM.

## Documentacion adicional

- [GLOSSARY.md](./GLOSSARY.md)
- [docs/requests.http](./docs/requests.http)
- [docs/http-client.env.json](./docs/http-client.env.json)
- [docs/restaurant-notification-svc.postman_collection.json](./docs/restaurant-notification-svc.postman_collection.json)
