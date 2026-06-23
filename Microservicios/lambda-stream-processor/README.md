# lambda-stream-processor

AWS Lambda standalone (single-module Maven, JAR shaded) que consume eventos de DynamoDB Streams sobre la tabla `restaurant-reservations` y los archiva en S3 como NDJSON, particionado por fecha, para analitica con Athena/Glue.

## Stack

- Java 21
- Maven (single-module)
- AWS Lambda Java Core 1.2.3 + Events 3.14.0
- AWS SDK v2 (S3)
- Jackson 2.18 (databind + jsr310)
- SLF4J + log4j2 con appender Lambda

## Arquitectura

```
DynamoDB (restaurant-reservations)
       |
       v
DynamoDB Streams (NEW_AND_OLD_IMAGES, batch 100, retry 3)
       |
       v
   Lambda  (DynamoStreamToS3Handler)
       |
       v
  S3 (restaurant-data-lake-XXXX)
   reservations/year=YYYY/month=MM/day=DD/<uuid>.json
       |
       v
  Glue Crawler -> Athena
```

## Como construir

```bash
mvn clean package
```

Genera `target/lambda-stream-processor.jar` (shaded uber-jar listo para subir a Lambda).

## Variables de entorno

| Variable | Default | Descripcion |
|---|---|---|
| `AWS_REGION` | `us-east-1` | Region (Lambda lo expone automaticamente) |
| `S3_DATA_LAKE_BUCKET` | `restaurant-data-lake-218852528992` | Bucket destino |

## Configuracion en Lambda

| Parametro | Valor recomendado |
|---|---|
| Runtime | Java 21 (Corretto) |
| Handler | `com.restaurant.lambda.DynamoStreamToS3Handler::handleRequest` |
| Memory | 512 MB |
| Timeout | 30 s |
| IAM role | `restaurant-lambda-role` (con `dynamodb:DescribeStream`, `dynamodb:GetRecords`, `s3:PutObject`) |
| Trigger | DynamoDB Streams de `restaurant-reservations`, batch size 100, retry 3 |

## Despliegue

```bash
aws lambda update-function-code \
  --function-name restaurant-stream-processor \
  --zip-file fileb://target/lambda-stream-processor.jar \
  --region us-east-1
```

## Testing

```bash
mvn test
```

JaCoCo HTML: `target/site/jacoco/index.html`.

## Documentacion adicional

- [GLOSSARY.md](./GLOSSARY.md) - explicacion del handler.
- [docs/sample-event.json](./docs/sample-event.json) - payload de ejemplo para `aws lambda invoke`.
