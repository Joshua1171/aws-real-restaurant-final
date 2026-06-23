package com.restaurant.lambda;

import com.amazonaws.services.lambda.runtime.events.DynamodbEvent;
import com.amazonaws.services.lambda.runtime.events.models.dynamodb.AttributeValue;
import com.amazonaws.services.lambda.runtime.events.models.dynamodb.StreamRecord;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Tests del handler.
 *
 * <p>Cubre:</p>
 * <ul>
 *   <li>Aplanado correcto de tipos S, N, BOOL.</li>
 *   <li>Procesa varios records y reporta el conteo.</li>
 *   <li>Si un record falla, se contabiliza como error y los demas siguen.</li>
 * </ul>
 */
class DynamoStreamToS3HandlerTest {

    private S3Client s3Client;
    private DynamoStreamToS3Handler handler;

    @BeforeEach
    void setUp() {
        s3Client = mock(S3Client.class);
        handler = new DynamoStreamToS3Handler(s3Client, new ObjectMapper(), "test-bucket");
    }

    @Test
    @DisplayName("flattenAttributes: mapea S, N, BOOL")
    void flattenAttributesBasicTypes() {
        final Map<String, AttributeValue> attrs = new HashMap<>();
        final AttributeValue stringValue = new AttributeValue();
        stringValue.setS("hello");
        attrs.put("greeting", stringValue);

        final AttributeValue numberValue = new AttributeValue();
        numberValue.setN("42");
        attrs.put("answer", numberValue);

        final AttributeValue booleanValue = new AttributeValue();
        booleanValue.setBOOL(Boolean.TRUE);
        attrs.put("flag", booleanValue);

        final Map<String, Object> flattened = handler.flattenAttributes(attrs);
        assertThat(flattened).containsEntry("greeting", "hello");
        assertThat(flattened).containsEntry("answer", "42");
        assertThat(flattened).containsEntry("flag", Boolean.TRUE);
    }

    @Test
    @DisplayName("handleRequest: procesa N registros y devuelve resumen")
    void handleRequestProcessesAll() {
        final DynamodbEvent event = new DynamodbEvent();
        event.setRecords(List.of(buildRecord("INSERT", "1"), buildRecord("MODIFY", "2")));

        final String response = handler.handleRequest(event, null);

        assertThat(response).contains("processed=2");
        assertThat(response).contains("errors=0");

        final ArgumentCaptor<PutObjectRequest> putCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client, times(2)).putObject(putCaptor.capture(), any(RequestBody.class));
        assertThat(putCaptor.getAllValues()).allSatisfy(req -> {
            assertThat(req.bucket()).isEqualTo("test-bucket");
            assertThat(req.key()).startsWith("reservations/");
        });
    }

    @Test
    @DisplayName("handleRequest: continua tras un error y reporta errores=1")
    void handleRequestRecoversFromOneError() {
        final DynamodbEvent.DynamodbStreamRecord brokenRecord = new DynamodbEvent.DynamodbStreamRecord();
        brokenRecord.setEventName("INSERT");
        brokenRecord.setEventID("broken");
        // dynamodb es null -> NPE en processSingleRecord -> contabilizado como error

        final DynamodbEvent event = new DynamodbEvent();
        event.setRecords(List.of(brokenRecord, buildRecord("INSERT", "ok")));

        final String response = handler.handleRequest(event, null);
        assertThat(response).contains("processed=1");
        assertThat(response).contains("errors=1");
    }

    private DynamodbEvent.DynamodbStreamRecord buildRecord(final String eventName, final String id) {
        final DynamodbEvent.DynamodbStreamRecord streamRecord = new DynamodbEvent.DynamodbStreamRecord();
        streamRecord.setEventName(eventName);
        streamRecord.setEventID("evt-" + id);
        streamRecord.setAwsRegion("us-east-1");

        final StreamRecord dynamoRecord = new StreamRecord();
        final Map<String, AttributeValue> newImage = new HashMap<>();
        final AttributeValue restaurantIdAttr = new AttributeValue();
        restaurantIdAttr.setS("rest-" + id);
        newImage.put("restaurant_id", restaurantIdAttr);
        dynamoRecord.setNewImage(newImage);
        streamRecord.setDynamodb(dynamoRecord);

        return streamRecord;
    }
}
