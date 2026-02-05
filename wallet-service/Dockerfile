# Multi-stage build for Wallet Service
# Build from project root: docker build -f wallet-service/Dockerfile -t wallet-service .

# Stage 1: Build
FROM eclipse-temurin:21-jdk AS builder
WORKDIR /app

# Install Maven
RUN apt-get update && apt-get install -y maven && rm -rf /var/lib/apt/lists/*

# Copy root pom
COPY pom.xml .

# Copy common module (shared dependency)
COPY common/pom.xml common/pom.xml
COPY common/src common/src

# Copy wallet-service module
COPY wallet-service/pom.xml wallet-service/pom.xml
COPY wallet-service/src wallet-service/src

# Build common module first, then wallet-service
RUN mvn clean install -pl common -DskipTests -B
RUN mvn clean package -pl wallet-service -DskipTests -B

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

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8082/actuator/health || exit 1

# Run with production profile
ENTRYPOINT ["java", "-jar", "app.jar", "--spring.profiles.active=prod"]
