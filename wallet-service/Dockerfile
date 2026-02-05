# Multi-stage build for Wallet Service
# Build from project root: docker build -f wallet-service/Dockerfile -t wallet-service .

# Stage 1: Build
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /app

# Copy common module (shared dependency)
COPY common/pom.xml common/pom.xml
COPY common/src common/src

# Copy wallet-service module
COPY wallet-service/pom.xml wallet-service/pom.xml
COPY wallet-service/src wallet-service/src

# Build common first (install to local Maven repo), then wallet-service.
# We build by module POM to avoid requiring the root multi-module reactor.
RUN mvn -f common/pom.xml -DskipTests install -B
RUN mvn -f wallet-service/pom.xml -DskipTests package -B

# Stage 2: Runtime
FROM eclipse-temurin:21-jre
WORKDIR /app

# Create non-root user for security
RUN groupadd -r apexpay && useradd -r -g apexpay apexpay

# Copy the built jar
COPY --from=builder /app/wallet-service/target/*.jar app.jar

# Change ownership to non-root user
RUN chown -R apexpay:apexpay /app
USER apexpay

# Expose port
EXPOSE 8082

# Run with production profile
ENTRYPOINT ["java", "-jar", "app.jar", "--spring.profiles.active=prod"]
