# GLOSSARY - restaurant-notification-svc

## NotificationApplication
**Responsabilidad.** Entry point.
**Superficie publica.** `main(String[])`.
**Cosas que saber.** El listener arranca solo gracias a `@SqsListener` + spring-cloud-aws-starter-sqs.

---

## listener.NotificationSqsListener
**Responsabilidad.** Consume mensajes de SQS, los deserializa (sobre SNS + evento) y los despacha a `EmailService`.
**Superficie publica.** `onNotificationMessage(String)` con `@SqsListener("${aws.sqs.queue-name}")`.
**Cosas que saber.** Si lanza excepcion, el mensaje vuelve a la cola. Tras los reintentos configurados pasa a la DLQ.
**Colaboraciones.** `EmailService`, `SmsService`, `ObjectMapper`.

---

## service.EmailService
**Responsabilidad.** Construye y envia emails HTML via SES.
**Superficie publica.** `sendReservationEmail(String, NotificationEvent)`.
**Cosas que saber.** En sandbox SES solo envia a destinatarios verificados. Subjects/cuerpo via `MessageSource` (i18n).
**Colaboraciones.** AWS `SesClient`.

---

## service.SmsService
**Responsabilidad.** Publica SMS directamente en SNS al numero (E.164).
**Superficie publica.** `sendReservationSms(String, NotificationEvent)`.
**Cosas que saber.** SNS SMS tiene limite de spending por defecto bajo en sandbox. Usar SMSType=Transactional.
**Colaboraciones.** AWS `SnsClient`.

---

## controller.HealthController
**Responsabilidad.** Endpoint informativo `/api/v1/notifications/status`.
**Superficie publica.** Un GET.
**Cosas que saber.** Existe solo porque ALB necesita un endpoint HTTP de "vida".

---

## dto.NotificationEvent
**Responsabilidad.** Record que representa el evento de negocio (mismo esquema que el publicado por reservation-svc).
**Superficie publica.** Componentes + 3 constantes de tipo de evento.
**Cosas que saber.** Tiene `@JsonIgnoreProperties(ignoreUnknown=true)` por compatibilidad si se anaden campos.

---

## dto.SnsMessageEnvelope
**Responsabilidad.** Captura la envoltura JSON de SNS al entregar a SQS.
**Superficie publica.** Componentes (`Type`, `MessageId`, `TopicArn`, `Message`, `Timestamp`).
**Cosas que saber.** Si la suscripcion SNS-&gt;SQS tiene "Raw Message Delivery", este envelope NO existe y hay que deserializar directo a `NotificationEvent`.

---

## config.AwsClientsConfig
**Responsabilidad.** Beans `SesClient` y `SnsClient`.
**Superficie publica.** Dos `@Bean`.

---

## config.SecurityConfig
**Responsabilidad.** Permite todo (no expone endpoints de negocio). Solo desactiva CSRF para testing.

---

## config.MessageSourceConfig
**Responsabilidad.** i18n en/es para subjects, cuerpos y SMS.

---

## Database migrations
Este servicio no usa base de datos. Si en el futuro se anade resolucion de email/telefono por `userId` desde una BD, anadir Flyway/Liquibase y documentar aqui.
