#!/bin/bash
#
# Build y push de las 3 imágenes Docker a Amazon ECR.
#
# Prerequisitos:
#   - AWS CLI configurado (aws configure)
#   - Docker corriendo
#   - Permisos ECR en tu usuario IAM
#
# Uso:
#   ./scripts/build-and-push-ecr.sh
#
set -e

AWS_REGION="us-east-1"
AWS_ACCOUNT_ID="218852528992"
ECR_REGISTRY="${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com"

SERVICES=(
    "restaurant-reservation-svc"
    "restaurant-search-svc"
    "restaurant-notification-svc"
)

echo "▶ Login en ECR..."
aws ecr get-login-password --region $AWS_REGION | \
    docker login --username AWS --password-stdin $ECR_REGISTRY

for SERVICE in "${SERVICES[@]}"; do
    echo ""
    echo "════════════════════════════════════════════"
    echo "  Procesando: $SERVICE"
    echo "════════════════════════════════════════════"

    # 1. Crear repositorio ECR si no existe
    echo "▶ Creando repo ECR $SERVICE (si no existe)..."
    aws ecr describe-repositories --repository-names $SERVICE --region $AWS_REGION 2>/dev/null \
        || aws ecr create-repository \
            --repository-name $SERVICE \
            --region $AWS_REGION \
            --image-scanning-configuration scanOnPush=true

    # 2. Build
    echo "▶ Build Docker $SERVICE..."
    docker build -f $SERVICE/Dockerfile -t $SERVICE:latest .

    # 3. Tag
    echo "▶ Tag $SERVICE:latest → ECR..."
    docker tag $SERVICE:latest $ECR_REGISTRY/$SERVICE:latest
    docker tag $SERVICE:latest $ECR_REGISTRY/$SERVICE:$(git rev-parse --short HEAD 2>/dev/null || echo "v1")

    # 4. Push
    echo "▶ Push a ECR..."
    docker push $ECR_REGISTRY/$SERVICE:latest
    docker push $ECR_REGISTRY/$SERVICE:$(git rev-parse --short HEAD 2>/dev/null || echo "v1")

    echo "✓ $SERVICE subido: $ECR_REGISTRY/$SERVICE:latest"
done

echo ""
echo "════════════════════════════════════════════"
echo "✓ Las 3 imágenes están en ECR"
echo "════════════════════════════════════════════"
