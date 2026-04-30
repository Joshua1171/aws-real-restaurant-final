# Multi-stage build: usa Maven solo para build, runtime ligero
# Stage 1: Build
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace

# Copiar parent pom primero para cachear dependencias
COPY pom.xml .
COPY restaurant-reservation-svc/pom.xml restaurant-reservation-svc/
COPY restaurant-search-svc/pom.xml restaurant-search-svc/
COPY restaurant-notification-svc/pom.xml restaurant-notification-svc/
COPY lambda-stream-processor/pom.xml lambda-stream-processor/

# Descargar dependencias offline (optimiza cache de Docker)
RUN mvn -pl restaurant-reservation-svc -am dependency:go-offline -B || true

# Copiar fuentes y compilar solo este módulo
COPY restaurant-reservation-svc/src restaurant-reservation-svc/src
RUN mvn -pl restaurant-reservation-svc -am package -DskipTests -B

# Stage 2: Runtime con JRE 21 mínima
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Usuario no-root por seguridad
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Copiar solo el jar compilado
COPY --from=build /workspace/restaurant-reservation-svc/target/restaurant-reservation-svc.jar app.jar

EXPOSE 8080

# Health check (ECS también hace su propio check al ALB)
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD wget --quiet --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# -XX:MaxRAMPercentage limita la heap al 75% de la RAM asignada por ECS
# -Djava.security.egd evita bloqueos por entropy en containers
ENTRYPOINT ["java", \
    "-XX:MaxRAMPercentage=75.0", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", "/app/app.jar"]
