# Multi-stage Dockerfile for Central Neuro Inventory Demo
# Build stage
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /workspace

# Copy Maven wrapper and POM
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B || true

# Copy source code and package application
COPY src ./src
RUN ./mvnw clean package -DskipTests -B

# Runtime stage
FROM eclipse-temurin:21-jre-alpine AS runner
WORKDIR /app

# Install curl/wget for healthcheck
RUN apk add --no-cache curl ca-certificates && \
    addgroup -S appgroup && adduser -S appuser -G appgroup

# Create volume/directory for file-based database or data storage
RUN mkdir -p /app/data && chown -R appuser:appgroup /app

# Copy built artifact
COPY --from=builder --chown=appuser:appgroup /workspace/target/*.jar /app/app.jar

USER appuser

ENV SERVER_PORT=8081 \
    SPRING_PROFILES_ACTIVE=docker \
    JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC"

EXPOSE 8081

HEALTHCHECK --interval=15s --timeout=5s --start-period=30s --retries=3 \
  CMD curl -f http://localhost:8081/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
