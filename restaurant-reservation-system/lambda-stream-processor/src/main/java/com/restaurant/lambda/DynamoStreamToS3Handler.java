package com.restaurant.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent;
import com.amazonaws.services.lambda.runtime.events.models.dynamodb.AttributeValue;
import com.amazonaws.services.lambda.runtime.events.models.dynamodb.StreamRecord;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Lambda que procesa eventos de DynamoDB Streams y los escribe en S3 para analítica.
 *
 * Trigger: DynamoDB Streams de la tabla restaurant-reservations
 *   - StreamViewType: NEW_AND_OLD_IMAGES (vemos estado anterior Y nuevo)
 *   - BatchSize: 100 registros por invocación (recomendado)
 *   - MaximumRetryAttempts: 3 (default)
 *
 * Destino: S3 bucket restaurant-data-lake-218852528992
 *   Particionado por fecha: /reservations/year=YYYY/month=MM/day=DD/*.json
 *   Formato: NDJSON (una línea por evento) para ser leído con Athena + Glue.
 *
 * Beneficios de esta arquitectura:
 *   1. Desacoplamiento: DynamoDB no conoce a S3/Athena
 *   2. Histórico completo para BI (DynamoDB es operacional, no analítico)
 *   3. Reproducibilidad: podemos reprocesar eventos leyendo de S3
 *
 * Runtime: Java 21 (Corretto) en Lambda
 * Memory: 512 MB (suficiente para batches de 100)
 * Timeout: 30 segundos
 * IAM Role: restaurant-lambda-role
 */
public class DynamoStreamToS3Handler implements RequestHandler<DynamodbEvent, String> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DynamoStreamToS3Handler.class);
    private static final DateTimeFormatter DATE_PARTITION_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd");
    private static final String S3_PREFIX = "reservations";
    private static final String CONTENT_TYPE_JSON = "application/json";

    private final S3Client s3Client;
    private final ObjectMapper objectMapper;
    private final String dataLakeBucket;

    public DynamoStreamToS3Handler() {
        final String regionEnv = System.getenv().getOrDefault("AWS_REGION", "us-east-1");
        this.s3Client = S3Client.builder().region(Region.of(regionEnv)).build();
        this.objectMapper = new ObjectMapper();
        this.dataLakeBucket = System.getenv().getOrDefault(
                "S3_DATA_LAKE_BUCKET", "restaurant-data-lake-218852528992");
    }

    @Override
    public String handleRequest(final DynamodbEvent dynamoStreamEvent, final Context lambdaContext) {
        final int totalRecords = dynamoStreamEvent.getRecords().size();
        LOGGER.info("Lambda invocada con {} registros. RequestId: {}",
                totalRecords, lambdaContext.getAwsRequestId());

        int successCount = 0;
        int errorCount = 0;

        for (final DynamodbEvent.DynamodbStreamRecord streamRecord : dynamoStreamEvent.getRecords()) {
            try {
                processSingleRecord(streamRecord);
                successCount++;
            } catch (final Exception processException) {
                LOGGER.error("Error procesando registro. eventId={}",
                        streamRecord.getEventID(), processException);
                errorCount++;
            }
        }

        LOGGER.info("Procesamiento completo. Éxitos: {}, Errores: {}", successCount, errorCount);
        return "OK. processed=%d errors=%d".formatted(successCount, errorCount);
    }

    private void processSingleRecord(final DynamodbEvent.DynamodbStreamRecord streamRecord) throws Exception {
        final String eventName = streamRecord.getEventName();
        final StreamRecord dynamoRecord = streamRecord.getDynamodb();

        final Map<String, Object> eventDocument = new HashMap<>();
        eventDocument.put("event_name", eventName);
        eventDocument.put("event_id", streamRecord.getEventID());
        eventDocument.put("aws_region", streamRecord.getAwsRegion());
        eventDocument.put("approximate_creation_datetime",
                dynamoRecord.getApproximateCreationDateTime() != null
                        ? dynamoRecord.getApproximateCreationDateTime().toString()
                        : null);

        if (dynamoRecord.getNewImage() != null) {
            eventDocument.put("new_image", flattenAttributes(dynamoRecord.getNewImage()));
        }
        if (dynamoRecord.getOldImage() != null) {
            eventDocument.put("old_image", flattenAttributes(dynamoRecord.getOldImage()));
        }

        final String jsonLine = objectMapper.writeValueAsString(eventDocument);
        writeToS3(jsonLine);
    }

    /**
     * Convierte los AttributeValue de DynamoDB en tipos Java primitivos para JSON limpio.
     * Solo maneja los tipos usados en la tabla (S, N, BOOL). Añadir más según el esquema.
     */
    private Map<String, Object> flattenAttributes(final Map<String, AttributeValue> attributes) {
        final Map<String, Object> flattened = new HashMap<>();
        for (final Map.Entry<String, AttributeValue> entry : attributes.entrySet()) {
            final AttributeValue attributeValue = entry.getValue();
            if (attributeValue.getS() != null) {
                flattened.put(entry.getKey(), attributeValue.getS());
            } else if (attributeValue.getN() != null) {
                flattened.put(entry.getKey(), attributeValue.getN());
            } else if (attributeValue.getBOOL() != null) {
                flattened.put(entry.getKey(), attributeValue.getBOOL());
            } else if (attributeValue.getNULL() != null && attributeValue.getNULL()) {
                flattened.put(entry.getKey(), null);
            } else {
                flattened.put(entry.getKey(), attributeValue.toString());
            }
        }
        return flattened;
    }

    private void writeToS3(final String jsonLine) {
        final String datePartition = LocalDate.now().format(DATE_PARTITION_FORMATTER);
        final String s3ObjectKey = "%s/%s/%s.json".formatted(S3_PREFIX, datePartition, UUID.randomUUID());

        final PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(dataLakeBucket)
                .key(s3ObjectKey)
                .contentType(CONTENT_TYPE_JSON)
                .build();

        s3Client.putObject(putObjectRequest, RequestBody.fromString(jsonLine));
        LOGGER.debug("Escrito en S3. bucket={}, key={}", dataLakeBucket, s3ObjectKey);
    }
}
