FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace

COPY pom.xml .
COPY restaurant-reservation-svc/pom.xml restaurant-reservation-svc/
COPY restaurant-search-svc/pom.xml restaurant-search-svc/
COPY restaurant-notification-svc/pom.xml restaurant-notification-svc/
COPY lambda-stream-processor/pom.xml lambda-stream-processor/

RUN mvn -pl restaurant-search-svc -am dependency:go-offline -B || true

COPY restaurant-search-svc/src restaurant-search-svc/src
RUN mvn -pl restaurant-search-svc -am package -DskipTests -B

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

COPY --from=build /workspace/restaurant-search-svc/target/restaurant-search-svc.jar app.jar

EXPOSE 8081

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD wget --quiet --tries=1 --spider http://localhost:8081/actuator/health || exit 1

ENTRYPOINT ["java", \
    "-XX:MaxRAMPercentage=75.0", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", "/app/app.jar"]
