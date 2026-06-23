# GLOSSARY - lambda-stream-processor

## DynamoStreamToS3Handler
**Responsabilidad.** Procesa un batch de eventos de DynamoDB Streams y escribe cada record en S3 como NDJSON.

**Superficie publica.**
- Constructor por defecto (lo invoca el runtime).
- Constructor `(S3Client, ObjectMapper, String bucket)` package-private para tests.
- `String handleRequest(DynamodbEvent, Context)`.

**Cosas que saber.**
- Procesa los registros uno por uno; si uno falla, los demas siguen y el resumen reporta `errors=N`.
- El particionado en S3 usa la fecha actual del handler (`LocalDate.now()`), no la fecha del evento. Para reprocesos historicos, considerar usar `approximate_creation_datetime`.
- Solo aplana tipos `S`, `N`, `BOOL`, `NULL`. Para `B`, `SS`, `NS`, `M`, `L`, ampliar `flattenAttributes`.
- El bucket destino se lee de la variable de entorno `S3_DATA_LAKE_BUCKET`.

**Colaboraciones.**
- AWS SDK v2 (`S3Client`).
- Jackson `ObjectMapper`.
- Tipos del runtime de Lambda (`DynamodbEvent`, `Context`).

---

## log4j2.xml
Configuracion de logging con el appender oficial de Lambda. Anade `AWSRequestId` al MDC para correlacionar entradas con la ejecucion.

---

## Database migrations
DynamoDB es NoSQL: no aplica Flyway. La estructura del Stream se rige por la tabla de origen, definida en `restaurant-reservation-svc`.
