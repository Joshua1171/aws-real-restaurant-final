#!/bin/bash
#
# Inicializa recursos en LocalStack para desarrollo local.
# Se ejecuta automáticamente cuando LocalStack arranca (ready.d/)
#
# Crea:
#   - 3 tablas DynamoDB (reservations, restaurants, users)
#   - Topic SNS + Cola SQS + suscripción
#   - Bucket S3 data-lake
#
set -e

AWS_ENDPOINT="http://localhost:4566"
AWS_REGION="us-east-1"
AWS_CMD="aws --endpoint-url=$AWS_ENDPOINT --region $AWS_REGION"

export AWS_ACCESS_KEY_ID=test
export AWS_SECRET_ACCESS_KEY=test

echo "▶ Creando tablas DynamoDB..."

$AWS_CMD dynamodb create-table \
    --table-name restaurant-reservations \
    --attribute-definitions \
        AttributeName=restaurant_id,AttributeType=S \
        AttributeName=reservation_datetime,AttributeType=S \
    --key-schema \
        AttributeName=restaurant_id,KeyType=HASH \
        AttributeName=reservation_datetime,KeyType=RANGE \
    --billing-mode PAY_PER_REQUEST \
    --stream-specification StreamEnabled=true,StreamViewType=NEW_AND_OLD_IMAGES \
    || echo "(ya existe)"

$AWS_CMD dynamodb create-table \
    --table-name restaurant-restaurants \
    --attribute-definitions \
        AttributeName=restaurant_id,AttributeType=S \
        AttributeName=city,AttributeType=S \
    --key-schema AttributeName=restaurant_id,KeyType=HASH \
    --global-secondary-indexes \
        "IndexName=city-index,KeySchema=[{AttributeName=city,KeyType=HASH}],Projection={ProjectionType=ALL}" \
    --billing-mode PAY_PER_REQUEST \
    || echo "(ya existe)"

$AWS_CMD dynamodb create-table \
    --table-name restaurant-users \
    --attribute-definitions AttributeName=user_id,AttributeType=S \
    --key-schema AttributeName=user_id,KeyType=HASH \
    --billing-mode PAY_PER_REQUEST \
    || echo "(ya existe)"

echo "▶ Creando topic SNS..."
TOPIC_ARN=$($AWS_CMD sns create-topic --name restaurant-notifications --query 'TopicArn' --output text)
echo "   TopicArn: $TOPIC_ARN"

echo "▶ Creando cola SQS..."
QUEUE_URL=$($AWS_CMD sqs create-queue --queue-name restaurant-notifications-queue --query 'QueueUrl' --output text)
QUEUE_ARN=$($AWS_CMD sqs get-queue-attributes --queue-url $QUEUE_URL --attribute-names QueueArn --query 'Attributes.QueueArn' --output text)
echo "   QueueArn: $QUEUE_ARN"

echo "▶ Suscribiendo SQS al topic SNS (fan-out)..."
$AWS_CMD sns subscribe --topic-arn $TOPIC_ARN --protocol sqs --notification-endpoint $QUEUE_ARN

echo "▶ Creando bucket S3 data-lake..."
$AWS_CMD s3 mb s3://restaurant-data-lake-218852528992 || echo "(ya existe)"

echo "✓ LocalStack listo para usar"
