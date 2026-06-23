# GLOSSARY - restaurant-payment-svc

## PaymentApplication
**Responsabilidad.** Entry point.
**Superficie publica.** `main(String[])`.

---

## controller.PaymentController
**Responsabilidad.** API REST `/api/v1/payments`. Todos los endpoints requieren JWT.
**Superficie publica.** `createPayment`, `getPayment`, `getPaymentsByReservation`, `capturePayment`, `refundPayment`.
**Cosas que saber.** `userId` viene del claim `sub` del JWT, nunca del body. `RefundPaymentRequest` es opcional (`required = false`).
**Colaboraciones.** `PaymentService`. Errores los maneja `GlobalExceptionHandler`.

---

## service.PaymentService
**Responsabilidad.** Logica de negocio. Orquesta DynamoDB + pasarela + SNS.
**Superficie publica.** `createPayment`, `getPayment`, `getPaymentsByReservation`, `capturePayment`, `refundPayment`.
**Cosas que saber.**
- `createPayment` NO publica evento (el pago aun no impacta dinero).
- `capturePayment`: si la pasarela rechaza, el pago queda en `FAILED` con `failureReason`, se publica `PAYMENT_FAILED` y se relanza la excepcion.
- Las transiciones invalidas se cortan antes de llamar a la pasarela y lanzan `InvalidPaymentStateException`.
**Colaboraciones.** `PaymentRepository`, `PaymentGatewayService`, `PaymentEventPublisherService`.

---

## service.PaymentGatewayService (interfaz)
**Responsabilidad.** Contrato con la pasarela real. Dos operaciones: `capture` y `refund`.
**Superficie publica.** Interfaz con dos metodos.
**Cosas que saber.** Usar `@Profile("prod")` en la implementacion real para desactivar el simulador en produccion.

---

## service.SimulatedPaymentGatewayService
**Responsabilidad.** Implementacion in-memory para `dev`/`test`/`default`.
**Superficie publica.** Implementa `PaymentGatewayService`.
**Cosas que saber.** Si `amountCents == 13` y `payment.simulator.failure-trigger-enabled=true` lanza `PaymentGatewayException` para facilitar tests del camino de error. Devuelve referencias con prefijo `SIM-` para que sea evidente.

---

## service.PaymentEventPublisherService
**Responsabilidad.** Publica eventos a SNS con el `MessageAttribute event_type`.
**Superficie publica.** `publishEvent(PaymentEvent)`.
**Cosas que saber.** Si la serializacion JSON falla lanza `IllegalStateException`. Cualquier otro error de SNS se relanza.
**Colaboraciones.** AWS `SnsClient`, Jackson `ObjectMapper`.

---

## repository.PaymentRepository
**Responsabilidad.** Acceso a DynamoDB (Enhanced) + GSI `reservation-index`.
**Superficie publica.** `save`, `findById`, `findByReservationId`.
**Cosas que saber.** El query sobre el GSI evita un Scan completo de la tabla.

---

## model.Payment
**Responsabilidad.** Entidad DynamoDB.
**Superficie publica.** Getters/setters anotados, `newPending(...)`, enums `Status` y `Method`.
**Cosas que saber.** `amountCents` se guarda como entero para evitar problemas de precision. No usa Lombok por compatibilidad con DynamoDB Enhanced.

---

## dto.CreatePaymentRequest
**Responsabilidad.** DTO de entrada al POST. Validacion via Bean Validation con mensajes i18n.
**Superficie publica.** Componentes con `@NotBlank`, `@NotNull`, `@Min`, `@Pattern`.

---

## dto.RefundPaymentRequest
**Responsabilidad.** Body opcional con motivo del reembolso.

---

## dto.PaymentResponse
**Responsabilidad.** DTO de salida. Provee `fromEntity`.

---

## dto.PaymentEvent
**Responsabilidad.** Payload publicado en SNS cuando cambia el estado.
**Superficie publica.** Componentes + 3 constantes (`PAYMENT_CAPTURED`, `PAYMENT_REFUNDED`, `PAYMENT_FAILED`).

---

## dto.ApiError
**Responsabilidad.** Body unico de error (RFC 7807 simplificado).

---

## exception.PaymentNotFoundException
404 desde el service.

## exception.InvalidPaymentStateException
409 cuando la transicion es invalida.

## exception.PaymentGatewayException
502 cuando la pasarela rechaza.

## exception.GlobalExceptionHandler
**Responsabilidad.** Mapea las excepciones a `ApiError` con HTTP status correcto. Usa `MessageSource` para titulos/detalles.

---

## config.DynamoDbConfig / SnsConfig
Beans del cliente respectivo con `DefaultCredentialsProvider`.

## config.SecurityConfig
JWT requerido en todos los endpoints excepto health/swagger.

## config.MessageSourceConfig
i18n en/es por `Accept-Language`.

## config.OpenApiConfig
Metadata + esquema `bearerAuth` para Swagger UI.

---

## Database migrations
Persistencia en DynamoDB (NoSQL). No aplica Flyway/Liquibase. La estructura de tabla y GSI esta documentada en `model.Payment` y en el README.
