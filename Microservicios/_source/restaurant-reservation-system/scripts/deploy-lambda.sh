#!/bin/bash
#
# Build y deploy de la Lambda DynamoStreamToS3.
#
# Pasos:
#   1. mvn package (crea el fat-jar con shade)
#   2. Crear función Lambda (o actualizarla)
#   3. Configurar el DynamoDB Streams como event source mapping
#
set -e

AWS_REGION="us-east-1"
AWS_ACCOUNT_ID="218852528992"
FUNCTION_NAME="restaurant-stream-processor"
HANDLER_CLASS="com.restaurant.lambda.DynamoStreamToS3Handler::handleRequest"
RUNTIME="java21"
ROLE_ARN="arn:aws:iam::${AWS_ACCOUNT_ID}:role/restaurant-lambda-role"
MEMORY_MB=512
TIMEOUT_SEC=30
S3_BUCKET="restaurant-data-lake-218852528992"
DYNAMO_TABLE_NAME="restaurant-reservations"

echo "▶ Compilando Lambda..."
cd "$(dirname "$0")/.."
mvn -pl lambda-stream-processor -am package -DskipTests

JAR_PATH="lambda-stream-processor/target/lambda-stream-processor.jar"

if [ ! -f "$JAR_PATH" ]; then
    echo "✗ Error: no se encontró el jar: $JAR_PATH"
    exit 1
fi

echo "▶ Verificando si la función Lambda existe..."
if aws lambda get-function --function-name $FUNCTION_NAME --region $AWS_REGION 2>/dev/null; then
    echo "▶ Función existe — actualizando código..."
    aws lambda update-function-code \
        --function-name $FUNCTION_NAME \
        --zip-file fileb://$JAR_PATH \
        --region $AWS_REGION
else
    echo "▶ Creando función Lambda..."
    aws lambda create-function \
        --function-name $FUNCTION_NAME \
        --runtime $RUNTIME \
        --role $ROLE_ARN \
        --handler $HANDLER_CLASS \
        --zip-file fileb://$JAR_PATH \
        --memory-size $MEMORY_MB \
        --timeout $TIMEOUT_SEC \
        --environment "Variables={S3_DATA_LAKE_BUCKET=$S3_BUCKET}" \
        --region $AWS_REGION
fi

echo "▶ Obteniendo ARN del stream DynamoDB..."
STREAM_ARN=$(aws dynamodb describe-table \
    --table-name $DYNAMO_TABLE_NAME \
    --region $AWS_REGION \
    --query 'Table.LatestStreamArn' \
    --output text)

if [ "$STREAM_ARN" == "None" ] || [ -z "$STREAM_ARN" ]; then
    echo "✗ Error: la tabla $DYNAMO_TABLE_NAME no tiene Streams habilitado"
    echo "  Habilítalo desde la consola: DynamoDB → Tabla → Exports and streams → Activar stream"
    exit 1
fi

echo "   Stream ARN: $STREAM_ARN"

echo "▶ Verificando event source mapping existente..."
MAPPING_UUID=$(aws lambda list-event-source-mappings \
    --function-name $FUNCTION_NAME \
    --region $AWS_REGION \
    --query "EventSourceMappings[?EventSourceArn=='$STREAM_ARN'].UUID | [0]" \
    --output text)

if [ "$MAPPING_UUID" == "None" ] || [ -z "$MAPPING_UUID" ]; then
    echo "▶ Creando trigger DynamoDB Streams → Lambda..."
    aws lambda create-event-source-mapping \
        --function-name $FUNCTION_NAME \
        --event-source-arn $STREAM_ARN \
        --starting-position LATEST \
        --batch-size 100 \
        --maximum-retry-attempts 3 \
        --region $AWS_REGION
else
    echo "   Trigger ya existente (UUID: $MAPPING_UUID)"
fi

echo ""
echo "✓ Lambda desplegada y conectada al stream"
echo "  Prueba: crea una reservación → los cambios llegan a s3://$S3_BUCKET/reservations/"
