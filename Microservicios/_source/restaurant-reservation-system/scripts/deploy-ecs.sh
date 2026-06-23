#!/bin/bash
#
# Deploy de los 3 microservicios a ECS Fargate.
#
# Prerequisitos:
#   - Las imágenes YA están en ECR (correr antes build-and-push-ecr.sh)
#   - El cluster restaurant-cluster existe
#   - Las subredes privadas y security group están creados
#
set -e

AWS_REGION="us-east-1"
AWS_ACCOUNT_ID="218852528992"
CLUSTER_NAME="restaurant-cluster"

# ⚠️ Reemplaza estos IDs por los reales de tu VPC
SUBNET_1="subnet-XXXXXXX1"  # private-subnet-1a
SUBNET_2="subnet-XXXXXXX2"  # private-subnet-1b
SECURITY_GROUP="sg-XXXXXXXX"

SERVICES=(
    "restaurant-reservation-svc"
    "restaurant-search-svc"
    "restaurant-notification-svc"
)

SCRIPT_DIR="$(dirname "$0")"

echo "▶ Verificando cluster $CLUSTER_NAME..."
if ! aws ecs describe-clusters --clusters $CLUSTER_NAME --region $AWS_REGION \
    --query 'clusters[0].status' --output text | grep -q ACTIVE; then
    echo "▶ Creando cluster $CLUSTER_NAME..."
    aws ecs create-cluster --cluster-name $CLUSTER_NAME --region $AWS_REGION
fi

for SERVICE in "${SERVICES[@]}"; do
    echo ""
    echo "════════════════════════════════════════════"
    echo "  Desplegando: $SERVICE"
    echo "════════════════════════════════════════════"

    SHORT_NAME=${SERVICE/restaurant-/}
    SHORT_NAME=${SHORT_NAME/-svc/}
    TASKDEF_FILE="$SCRIPT_DIR/taskdef-${SHORT_NAME}.json"

    echo "▶ Registrando task definition..."
    TASK_ARN=$(aws ecs register-task-definition \
        --cli-input-json file://$TASKDEF_FILE \
        --region $AWS_REGION \
        --query 'taskDefinition.taskDefinitionArn' \
        --output text)
    echo "   $TASK_ARN"

    echo "▶ Verificando si service existe..."
    if aws ecs describe-services --cluster $CLUSTER_NAME --services $SERVICE --region $AWS_REGION \
        --query 'services[0].status' --output text 2>/dev/null | grep -q ACTIVE; then
        echo "▶ Actualizando servicio existente..."
        aws ecs update-service \
            --cluster $CLUSTER_NAME \
            --service $SERVICE \
            --task-definition $TASK_ARN \
            --region $AWS_REGION \
            --force-new-deployment
    else
        echo "▶ Creando nuevo servicio..."
        aws ecs create-service \
            --cluster $CLUSTER_NAME \
            --service-name $SERVICE \
            --task-definition $TASK_ARN \
            --desired-count 1 \
            --launch-type FARGATE \
            --network-configuration "awsvpcConfiguration={subnets=[$SUBNET_1,$SUBNET_2],securityGroups=[$SECURITY_GROUP],assignPublicIp=DISABLED}" \
            --region $AWS_REGION
    fi

    echo "✓ $SERVICE desplegado"
done

echo ""
echo "════════════════════════════════════════════"
echo "✓ Los 3 servicios están en ECS Fargate"
echo "════════════════════════════════════════════"
echo ""
echo "Monitoreo:"
echo "  aws ecs describe-services --cluster $CLUSTER_NAME --services ${SERVICES[@]} --region $AWS_REGION"
echo "  Logs: CloudWatch → Log groups → /ecs/restaurant-*"
