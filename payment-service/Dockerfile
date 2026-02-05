# Multi-stage build for Payment Service
# Build from project root: docker build -f payment-service/Dockerfile -t payment-service .

# Stage 1: Build
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /app

# Copy common module (shared dependency)
COPY common/pom.xml common/pom.xml
COPY common/src common/src

# Copy payment-service module
COPY payment-service/pom.xml payment-service/pom.xml
COPY payment-service/src payment-service/src

# Build common first (install to local Maven repo), then payment-service.
# We build by module POM to avoid requiring the root multi-module reactor.
RUN mvn -f common/pom.xml -DskipTests install -B
RUN mvn -f payment-service/pom.xml -DskipTests package -B

# Stage 2: Runtime
FROM eclipse-temurin:21-jre
WORKDIR /app

# Create non-root user for security
RUN groupadd -r apexpay && useradd -r -g apexpay apexpay

# Copy the built jar
COPY --from=builder /app/payment-service/target/*.jar app.jar

# Change ownership to non-root user
RUN chown -R apexpay:apexpay /app
USER apexpay

# Expose port
EXPOSE 8083

# Run with production profile
ENTRYPOINT ["java", "-jar", "app.jar", "--spring.profiles.active=prod"]
