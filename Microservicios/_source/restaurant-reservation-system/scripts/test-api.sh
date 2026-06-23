#!/bin/bash
#
# Pruebas end-to-end de los microservicios.
#
# Uso:
#   ./scripts/test-api.sh local   # contra docker-compose
#   ./scripts/test-api.sh aws     # contra ALB en AWS
#
set -e

MODE=${1:-local}

if [ "$MODE" == "local" ]; then
    RESERVATION_URL="http://localhost:8080"
    SEARCH_URL="http://localhost:8081"
    NOTIFICATION_URL="http://localhost:8082"
elif [ "$MODE" == "aws" ]; then
    # ⚠️ Reemplaza con el DNS de tu ALB después del deploy
    ALB_DNS="restaurant-alb-XXXXX.us-east-1.elb.amazonaws.com"
    RESERVATION_URL="http://$ALB_DNS"
    SEARCH_URL="http://$ALB_DNS"
    NOTIFICATION_URL="http://$ALB_DNS"
else
    echo "Uso: $0 [local|aws]"
    exit 1
fi

# Token JWT de prueba (en local apunta a Cognito de LocalStack o token dummy)
# Para AWS real, obtén un token via:
#   aws cognito-idp admin-initiate-auth ...
JWT_TOKEN=${JWT_TOKEN:-"eyJhbGci..."}

echo "════════════════════════════════════════════"
echo "  TEST 1: Health checks"
echo "════════════════════════════════════════════"
curl -s $RESERVATION_URL/actuator/health | jq .
curl -s $SEARCH_URL/actuator/health | jq .
curl -s $NOTIFICATION_URL/actuator/health | jq .

echo ""
echo "════════════════════════════════════════════"
echo "  TEST 2: Crear un restaurante"
echo "════════════════════════════════════════════"
RESTAURANT=$(curl -s -X POST $SEARCH_URL/api/v1/restaurants \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer $JWT_TOKEN" \
    -d '{
        "name": "La Casa del Mole",
        "description": "Cocina mexicana tradicional",
        "city": "Toluca",
        "address": "Av. Independencia 123",
        "cuisineType": "MEXICAN",
        "priceRange": "MODERATE",
        "seatingCapacity": 50,
        "openingHours": ["Mon-Fri 12:00-22:00", "Sat-Sun 11:00-23:00"],
        "phone": "+527221234567",
        "email": "contacto@casadelmole.mx"
    }')
echo $RESTAURANT | jq .
RESTAURANT_ID=$(echo $RESTAURANT | jq -r .restaurantId)

echo ""
echo "════════════════════════════════════════════"
echo "  TEST 3: Buscar restaurantes en Toluca"
echo "════════════════════════════════════════════"
curl -s "$SEARCH_URL/api/v1/restaurants/search?city=Toluca" | jq .

echo ""
echo "════════════════════════════════════════════"
echo "  TEST 4: Crear una reservación"
echo "════════════════════════════════════════════"
RESERVATION=$(curl -s -X POST $RESERVATION_URL/api/v1/reservations \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer $JWT_TOKEN" \
    -d "{
        \"restaurantId\": \"$RESTAURANT_ID\",
        \"reservationDatetime\": \"2026-05-01T20:00:00Z\",
        \"partySize\": 4,
        \"specialRequests\": \"Mesa cerca de la ventana\"
    }")
echo $RESERVATION | jq .

echo ""
echo "════════════════════════════════════════════"
echo "  TEST 5: Confirmar reservación (dueño)"
echo "════════════════════════════════════════════"
curl -s -X PUT "$RESERVATION_URL/api/v1/reservations/$RESTAURANT_ID/2026-05-01T20:00:00Z/confirm" \
    -H "Authorization: Bearer $JWT_TOKEN" | jq .

echo ""
echo "✓ Tests completados"
echo "  Revisa los logs del notification-svc para ver el procesamiento del evento SNS→SQS"
