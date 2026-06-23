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
 * Lambda que procesa eventos de DynamoDB Streams y los archiva en S3 para analitica.
 *
 * <p>Trigger: DynamoDB Streams de la tabla {@code restaurant-reservations}
 * con {@code StreamViewType=NEW_AND_OLD_IMAGES}, batch size 100, retry 3.</p>
 *
 * <p>Destino: bucket {@code restaurant-data-lake-218852528992}, particionado
 * por fecha: {@code /reservations/year=YYYY/month=MM/day=DD/*.json}.</p>
 *
 * <p>Formato: NDJSON (un objeto JSON por archivo) consumible con Athena + Glue Crawler.</p>
 *
 * <p>Runtime: Java 21 Corretto. Memory: 512 MB. Timeout: 30s.
 * IAM Role: {@code restaurant-lambda-role}.</p>
 *
 * @author Joshua
 * @since 1.0.0
 */
public class DynamoStreamToS3Handler implements RequestHandler<DynamodbEvent, String> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DynamoStreamToS3Handler.class);
    private static final DateTimeFormatter DATE_PARTITION_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd");
    private static final String S3_PREFIX = "reservations";
    private static final String CONTENT_TYPE_JSON = "application/json";

    private final S3Client s3Client;
    private final ObjectMapper objectMapper;
    private final String dataLakeBucket;

    /**
     * Constructor por defecto invocado por Lambda runtime.
     */
    public DynamoStreamToS3Handler() {
        this(buildDefaultS3Client(),
                new ObjectMapper(),
                System.getenv().getOrDefault("S3_DATA_LAKE_BUCKET", "restaurant-data-lake-218852528992"));
    }

    /**
     * Constructor visible para tests.
     *
     * @param s3Client       cliente S3 (puede ser mock).
     * @param objectMapper   serializador.
     * @param dataLakeBucket bucket destino.
     */
    DynamoStreamToS3Handler(final S3Client s3Client, final ObjectMapper objectMapper, final String dataLakeBucket) {
        this.s3Client = s3Client;
        this.objectMapper = objectMapper;
        this.dataLakeBucket = dataLakeBucket;
    }

    private static S3Client buildDefaultS3Client() {
        final String regionEnv = System.getenv().getOrDefault("AWS_REGION", "us-east-1");
        return S3Client.builder().region(Region.of(regionEnv)).build();
    }

    /**
     * Punto de entrada de Lambda.
     *
     * @param dynamoStreamEvent batch de eventos de DynamoDB Streams.
     * @param lambdaContext     contexto de la invocacion.
     * @return resumen "OK. processed=N errors=M".
     */
    @Override
    public String handleRequest(final DynamodbEvent dynamoStreamEvent, final Context lambdaContext) {
        final int totalRecords = dynamoStreamEvent.getRecords().size();
        LOGGER.info("Lambda invocada. records={}, requestId={}",
                totalRecords, lambdaContext != null ? lambdaContext.getAwsRequestId() : "n/a");

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

        LOGGER.info("Procesamiento completo. exitos={}, errores={}", successCount, errorCount);
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
     * Aplana {@link AttributeValue} a tipos Java primitivos.
     *
     * <p>Solo cubre los tipos en uso: S, N, BOOL, NULL. Anadir mas tipos
     * (B, SS, NS, M, L) cuando el esquema los necesite.</p>
     *
     * @param attributes mapa nombre -&gt; AttributeValue.
     * @return mapa con valores Java planos.
     */
    Map<String, Object> flattenAttributes(final Map<String, AttributeValue> attributes) {
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
