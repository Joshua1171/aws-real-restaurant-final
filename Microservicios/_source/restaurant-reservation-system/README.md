# Restaurant Reservation System

Sistema de reservaciones de restaurantes desplegable en AWS, basado en microservicios Spring Boot + DynamoDB + SNS/SQS + ECS Fargate + Lambda.

---

## 📐 Arquitectura

```
┌──────────────┐     ┌────────┐     ┌────────────┐     ┌──────┐     ┌────────────┐
│  Cliente     │────▶│Route53 │────▶│CloudFront  │────▶│ WAF  │────▶│  Cognito   │
│(web/móvil)   │     └────────┘     │  (cache)   │     └──────┘     │User Pools  │
└──────────────┘                    └────────────┘                   └────────────┘
                                           │
                                           ▼
                                    ┌────────────┐
                                    │    ALB     │
                                    │ (pública)  │
                                    └─────┬──────┘
                                          │
                 ┌────────────────────────┼────────────────────────┐
                 ▼                        ▼                        ▼
        ┌──────────────────┐   ┌──────────────────┐   ┌──────────────────┐
        │ reservation-svc  │   │   search-svc     │   │ notification-svc │
        │   (8080) ECS     │   │   (8081) ECS     │   │   (8082) ECS     │
        └────────┬─────────┘   └────────┬─────────┘   └────────▲─────────┘
                 │                      │                      │
                 │ ┌────────────────────┘                      │
                 ▼ ▼                                           │
        ┌──────────────────┐      ┌──────────────────┐         │
        │    DynamoDB      │      │  ElastiCache     │         │
        │ (3 tablas)       │      │  Redis (cache)   │         │
        └────────┬─────────┘      └──────────────────┘         │
                 │                                             │
                 │ Streams                                     │
                 ▼                                             │
        ┌──────────────────┐                                   │
        │ Lambda (Java 21) │                                   │
        │stream-processor  │                                   │
        └────────┬─────────┘                                   │
                 ▼                                             │
        ┌──────────────────┐                                   │
        │  S3 Data Lake    │◀─── Athena ─── QuickSight         │
        │  (particionado)  │                                   │
        └──────────────────┘                                   │
                                                               │
        reservation-svc ──▶ SNS ──▶ SQS ───────────────────────┘
                          (topic)  (queue)
                                      │
                                      ▼
                               notification-svc
                                      │
                                      ▼
                                   SES (email)
```

---

## 📦 Módulos

| Módulo | Puerto | Descripción |
|---|---|---|
| `restaurant-reservation-svc` | 8080 | CRUD de reservaciones. Publica en SNS |
| `restaurant-search-svc` | 8081 | Búsqueda de restaurantes. Cache Caffeine |
| `restaurant-notification-svc` | 8082 | Consume SQS → envía emails/SMS |
| `lambda-stream-processor` | — | Lambda Java: DynamoDB Streams → S3 |

---

## 🚀 Despliegue paso a paso

### Prerequisitos

- Java 21 (Temurin recomendado)
- Maven 3.9+
- Docker 24+
- AWS CLI v2 configurado (`aws configure`)
- Cuenta AWS (account ID: `218852528992`, región: `us-east-1`)

### 1️⃣ Desarrollo local con LocalStack

```bash
# Arrancar LocalStack + 3 microservicios
docker-compose up -d --build

# Esperar ~30s, luego probar
./scripts/test-api.sh local
```

LocalStack emula DynamoDB, SNS, SQS, S3. El script `init-localstack.sh` crea tablas y colas automáticamente.

### 2️⃣ Despliegue a AWS real

**Paso A: Infraestructura base** (ya creada en tu cuenta)

Estos recursos ya existen en tu cuenta según el manual:

- VPC `vpc-0084b69d18946f54f`
- 3 roles IAM (`restaurant-ecs-task-role`, `restaurant-lambda-role`, `restaurant-eventbridge-role`)
- 3 tablas DynamoDB
- Bucket S3 `restaurant-data-lake-218852528992`
- 2 Cognito User Pools
- SNS topic `restaurant-notifications` + SQS `restaurant-notifications-queue`

**Paso B: Habilitar DynamoDB Streams** (si no está)

```bash
aws dynamodb update-table \
    --table-name restaurant-reservations \
    --stream-specification StreamEnabled=true,StreamViewType=NEW_AND_OLD_IMAGES \
    --region us-east-1
```

**Paso C: Verificar email en SES**

```bash
aws ses verify-email-identity --email-address TU_EMAIL@gmail.com --region us-east-1
# Revisa tu bandeja y haz clic en el enlace de verificación
```

**Paso D: Build y push a ECR**

```bash
./scripts/build-and-push-ecr.sh
```

Este script crea 3 repositorios ECR y sube las imágenes.

**Paso E: Deploy de la Lambda**

```bash
./scripts/deploy-lambda.sh
```

Crea la función Lambda `restaurant-stream-processor` y la conecta al stream de DynamoDB.

**Paso F: Deploy a ECS Fargate**

Antes, edita `scripts/deploy-ecs.sh` y reemplaza:

```bash
SUBNET_1="subnet-XXXXXXX1"   # Tu private-subnet-1a real
SUBNET_2="subnet-XXXXXXX2"   # Tu private-subnet-1b real
SECURITY_GROUP="sg-XXXXXXXX" # Tu SG que permita 8080-8082 y acceso a DynamoDB
```

Obtén los IDs con:

```bash
aws ec2 describe-subnets --filters "Name=vpc-id,Values=vpc-0084b69d18946f54f" \
    --query 'Subnets[*].[SubnetId,Tags[?Key==`Name`]|[0].Value]' --output table
```

Luego ejecuta:

```bash
./scripts/deploy-ecs.sh
```

**Paso G: Crear el ALB** (una vez)

Desde la consola EC2 → Load Balancers → Create ALB:
- Public subnets
- Target groups: 3 (uno por servicio, tipo `ip`, port 8080/8081/8082)
- Health check path: `/actuator/health`
- Listener rules por path:
  - `/api/v1/reservations/*` → reservation TG
  - `/api/v1/restaurants/*` → search TG
  - `/api/v1/notifications/*` → notification TG

---

## 🧪 Pruebas

### Obtener token JWT de Cognito

```bash
aws cognito-idp admin-initiate-auth \
    --user-pool-id us-east-1_ZLhzcDygK \
    --client-id 27ogna3fscqmegk7q95md2l7vj \
    --auth-flow ADMIN_USER_PASSWORD_AUTH \
    --auth-parameters USERNAME=tu-email@ejemplo.com,PASSWORD=TuPassword123! \
    --region us-east-1
```

Copia el `IdToken` y expórtalo:

```bash
export JWT_TOKEN="eyJhbGci..."
./scripts/test-api.sh aws
```

### Swagger UI

- http://localhost:8080/swagger-ui.html (reservation)
- http://localhost:8081/swagger-ui.html (search)

---

## 🔐 Seguridad

- Todas las credenciales AWS se obtienen via **IAM Role de la task ECS** (nunca hardcoded)
- Los JWT de Cognito se validan contra el JWKS endpoint automáticamente
- CSRF deshabilitado (API stateless con Bearer tokens)
- Containers corren con usuario **no-root** (`spring`)
- HealthCheck path `/actuator/health` expuesto, el resto de actuator requiere auth

---

## 💰 Costos estimados

| Recurso | Costo diario | Notas |
|---|---|---|
| 3× ECS Fargate (0.5 vCPU, 1GB) | ~$2.88 | On-demand |
| Lambda (invocaciones bajas) | ~$0.01 | Free Tier cubre 1M req/mes |
| DynamoDB on-demand | ~$0.00-$0.10 | Pay-per-request |
| SNS + SQS | ~$0.00 | Free Tier: 1M requests |
| S3 | ~$0.02 | <1GB |
| ALB | ~$0.55 | Fixed cost |
| **Total** | **~$3.50/día** | Sin NAT Gateway |

Para reducir a $0: detén los servicios ECS (`desired-count=0`) cuando no practiques.

---

## 🔎 Troubleshooting

**Lambda no recibe eventos:**
```bash
aws lambda list-event-source-mappings --function-name restaurant-stream-processor
# Verifica State=Enabled y LastProcessingResult=OK
```

**ECS task falla con "CannotPullContainerError":**
- Verifica que la subnet tenga NAT Gateway o VPC endpoint de ECR
- Alternativamente, pon las tasks en subnets públicas con `assignPublicIp=ENABLED` (NO recomendado para prod)

**SQS listener no consume:**
- Verifica la policy del queue: debe permitir `sns:SendMessage` desde el topic ARN
- Revisa logs en CloudWatch: `/ecs/restaurant-notification-svc`

---

## 📚 Documentos relacionados

- `AWS-Lab-Manual-Restaurant.docx` — Manual de configuración AWS paso a paso
- Simuladores HTML por servicio (VPC, IAM, DynamoDB, S3, Cognito, SNS, SQS, EventBridge, ECS)

---

## 📝 Licencia

Proyecto educativo para certificación AWS SAA-C03.
