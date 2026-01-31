# ApexPay - Distributed Payment System

<!-- Badges - Update URLs with your actual repo -->

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Spring Cloud](https://img.shields.io/badge/Spring%20Cloud-2025.1.0-brightgreen.svg)](https://spring.io/projects/spring-cloud)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

> A production-ready distributed payment system built with microservices architecture, demonstrating enterprise-grade patterns for scalability, resilience, and security.

<!-- Screenshot/Demo GIF placeholder -->
<!-- ![ApexPay Demo](docs/images/demo.gif) -->

## Table of Contents

- [Overview](#overview)
- [Key Features](#key-features)
- [Architecture](#architecture)
- [Tech Stack](#tech-stack)
- [Services](#services)
- [Getting Started](#getting-started)
- [API Documentation](#api-documentation)
- [Project Structure](#project-structure)
- [Design Decisions](#design-decisions)
- [Testing](#testing)
- [Future Improvements](#future-improvements)
- [License](#license)

## Overview

ApexPay is a distributed payment processing system built with microservices architecture. It enables users to create digital wallets, manage balances, and process payments securely across multiple independent services. The system demonstrates enterprise-grade patterns including service discovery, API gateway routing, JWT-based authentication with token rotation, circuit breakers for resilience, and optimistic locking for concurrent transaction handling.

Built with Spring Boot and Spring Cloud, ApexPay showcases how to architect scalable, fault-tolerant distributed systems while maintaining data consistency and security across service boundaries.

**Why I built this:**
I built this project to gain hands-on experience with microservices architecture. Through building ApexPay, I learned how to:

- Decompose a monolithic application into independently deployable services
- Implement inter-service communication patterns using service discovery and API gateways
- Design secure, stateless authentication that works across distributed services
- Handle distributed transactions and ensure data consistency across service boundaries
- Apply resilience patterns like circuit breakers to build fault-tolerant systems

This project demonstrates my understanding of enterprise-grade microservices patterns and distributed systems principles.

## Key Features

<!-- List 5-8 main features with brief descriptions -->

- **Microservices Architecture** - Independently deployable services with clear boundaries
- **Service Discovery** - Dynamic service registration and discovery with Netflix Eureka
- **API Gateway** - Centralized routing and authentication
- **JWT Authentication** - Secure, stateless authentication with refresh token rotation
- **Wallet Management** - Digital wallet with balance management and transaction history
- **Payment Processing** - Idempotent payment operations with a two-phase commit (2PC) pattern and provider abstraction
- **Mock Payment Provider** - Configurable simulation of external gateways (Stripe/PayPal) with latency and deterministic test tokens
- **Circuit Breaker Pattern** - Resilience4j automatically opens circuits when services fail, preventing cascading failures
- **Distributed Transactions** - Optimistic locking and a reservation-based 2PC flow for cross-service consistency
- **Resilience & Retries** - Exponential backoff for transient payment provider failures

## Architecture

<!-- High-level architecture diagram -->
<!-- You can create one using draw.io, Mermaid, or ASCII art -->

```
┌─────────────────────────────────────────────────────────────────────────┐
│                              Client Applications                         │
└─────────────────────────────────┬───────────────────────────────────────┘
                                  │
                                  ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                         API Gateway (Port 9000)                          │
│                    - JWT Validation                                      │
│                    - Route Management                                    │
│                    - Circuit Breaker                                     │
└─────────────────────────────────┬───────────────────────────────────────┘
                                  │
                                  ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                      Discovery Server (Port 8761)                        │
│                         Netflix Eureka                                   │
└─────────────────────────────────────────────────────────────────────────┘
                                  │
          ┌───────────────────────┼───────────────────────┐
          ▼                       ▼                       ▼
┌──────────────────┐   ┌──────────────────┐   ┌──────────────────┐
│   User Service   │   │  Wallet Service  │   │ Payment Service  │
│   (Port 8081)    │   │    (Port 8082)   │   │   (Port 8083)    │
│ - Registration   │   │ - Balance Mgmt   │   │ - Payment Init   │
│ - Authentication │   │ - Transactions   │   │ - Provider Integ │
│ - JWT Issuance   │   │ - Transfers      │   │ - Idempotency    │
└────────┬─────────┘   └────────┬─────────┘   └────────┬─────────┘
         │                      │                      │
         └──────────────────────┼──────────────────────┘
                                ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                           PostgreSQL Database                            │
└─────────────────────────────────────────────────────────────────────────┘
```

### Request Flow

**Example: User initiates and completes a payment**

1. **Initiation (Idempotent):**
   - Client sends POST `/api/v1/payment/initiate` with a `client_request_id`.
   - Payment service creates a record in `INITIATED` status. If the ID exists, it returns the existing record.

2. **Phase 1: Fund Reservation:**
   - Client calls POST `/api/v1/payment/{id}/process`.
   - Payment service calls `wallet-service` to **reserve** funds.
   - Wallet service moves funds from `balance` to `reserved_balance` and creates a `PENDING` transaction.

3. **Phase 2: External Charge:**
   - Payment service calls the `MockPaymentProviderClient` to charge the external gateway.
   - Implements **exponential backoff retries** (1s, 2s, 4s) for transient network or provider errors.

4. **Phase 3: Confirmation/Rollback:**
   - **On Success:** Payment service calls `wallet-service` to **confirm** reservation. Wallet deducts from `balance` and clears `reserved_balance`.
   - **On Failure:** Payment service calls `wallet-service` to **cancel** reservation. Funds are released back to the available balance.

5. **Status Polling (Optional):**
   - If the provider returns `PENDING`, the client can poll `/api/v1/payment/{id}/status` to trigger a status sync with the provider.

**Error Scenarios:**

- **Service Down:** Circuit breaker opens, gateway returns fallback response (503).
- **Token Invalid:** Gateway returns 401 Unauthorized before routing.
- **Insufficient Balance:** Wallet service returns 403 with error code 5003.
- **Duplicate Request:** Payment service returns existing payment (idempotent response).

## Tech Stack

| Category               | Technology                             | Rationale                                                                                                                                                                                                                                                                                                                         |
| ---------------------- | -------------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **Language**           | Java 21                                | **Why Java 21?** Java 21 is an LTS release with modern features like pattern matching, records, and virtual threads. Compared to Java 17, it offers better performance and developer experience. I chose Java over Kotlin/Go because of Spring Boot's excellent Java support and the extensive ecosystem.                         |
| **Framework**          | Spring Boot 4.0, Spring Cloud 2025.1.0 | **Why Spring Boot?** Spring Boot provides auto-configuration, embedded servers, and production-ready features out of the box. Compared to Quarkus/Micronaut, Spring Boot has a larger community and more comprehensive documentation. Spring Cloud integrates seamlessly for microservices patterns.                              |
| **Service Discovery**  | Netflix Eureka                         | **Why Eureka over Consul/Zookeeper?** Eureka is simple to set up, integrates natively with Spring Cloud, and provides a self-preservation mode for resilience. Consul offers more features (service mesh, KV store) but adds complexity. For this learning project, Eureka's simplicity was ideal.                                |
| **API Gateway**        | Spring Cloud Gateway                   | **Why Spring Cloud Gateway over Zuul/Kong?** Spring Cloud Gateway is reactive (built on WebFlux), supports programmatic route configuration, and integrates with Spring Cloud ecosystem. Zuul 1.x is blocking and deprecated. Kong is powerful but requires separate infrastructure.                                              |
| **Security**           | Spring Security, JWT (jjwt)            | **Why JWT over OAuth2/Session?** JWT enables stateless authentication, perfect for microservices. RS256 (public/private key) allows the gateway to validate tokens without calling the auth service. Sessions would require sticky sessions or shared session store, adding complexity.                                           |
| **Database**           | PostgreSQL                             | **Why PostgreSQL over MySQL/MongoDB?** PostgreSQL offers ACID compliance, JSON support, and excellent performance. For financial transactions, ACID is critical. MongoDB's eventual consistency isn't suitable for payment data. PostgreSQL's schema separation allows database-per-service pattern while sharing infrastructure. |
| **ORM**                | Spring Data JPA                        | **Why JPA over MyBatis/JDBC?** JPA reduces boilerplate, provides optimistic locking support (via `@Version`), and integrates seamlessly with Spring. MyBatis offers more SQL control but requires more code. For this project, JPA's productivity benefits outweighed the need for fine-grained SQL control.                      |
| **Resilience**         | Resilience4j                           | **Why Resilience4j over Hystrix?** Resilience4j is actively maintained, functional programming style, and works with both blocking and reactive code. Hystrix is in maintenance mode. Resilience4j's circuit breaker, retry, and rate limiter patterns are essential for distributed systems.                                     |
| **Inter-service Comm** | OpenFeign                              | **Why OpenFeign over RestTemplate/WebClient?** OpenFeign provides declarative HTTP clients with built-in load balancing via Eureka. RestTemplate is deprecated. WebClient is reactive but requires more boilerplate. OpenFeign's annotation-based approach is cleaner for synchronous calls.                                      |
| **Build Tool**         | Maven                                  | **Why Maven over Gradle?** Maven has better IDE support, simpler dependency management, and is more widely adopted in enterprise. Gradle is faster but has a steeper learning curve. For consistency and simplicity, Maven was chosen.                                                                                            |
| **Containerization**   | Docker (planned)                       | **Why Docker?** Docker enables consistent environments across development, testing, and production. It simplifies deployment and scaling of microservices. Kubernetes orchestration is planned for production deployment.                                                                                                         |

## Services

### Discovery Server (Port 8761)

Netflix Eureka server for service registration and discovery.

### API Gateway (Port 9000)

- JWT token validation using public key
- Dynamic routing to microservices
- Circuit breaker with fallback responses
- Request/response logging

### User Service (Port 8081)

- User registration and authentication
- JWT access token and refresh token generation
- Secure password hashing with BCrypt
- Refresh token rotation and revocation with family tracking
- Token reuse detection and cascade revocation

### Wallet Service (Port 8082)

- Wallet creation and management
- Balance inquiries with optimistic locking
- Fund transfers between wallets
- Transaction history tracking
- Fund reservation for payments
- Concurrent update handling with retry mechanism

### Payment Service (Port 8083)

- Payment initiation and processing using 2PC (Reserve-Confirm/Cancel)
- Payment provider abstraction layer with Mock implementation
- Idempotency key handling via `client_request_id`
- Exponential backoff retry logic for provider calls
- Support for `PENDING` payment status polling and reconciliation
- Integration with wallet service for fund reservation

### Common Module

Shared utilities, DTOs, enums, and exception handling across services.

## Getting Started

### Prerequisites

- Java 21+ (LTS version recommended)
- Maven 3.9+
- PostgreSQL 15+
- IDE with Spring Boot support (IntelliJ IDEA, VS Code, or Eclipse)

### Environment Variables

Create a `.env` file or set the following environment variables:

```bash
# Database Configuration
DB_URL=jdbc:postgresql://localhost:5432/apexpay
DB_USERNAME=your_username
DB_PASSWORD=your_password
DB_DRIVER=org.postgresql.Driver

# Eureka Service Discovery
EUREKA_SERVER_URL=http://localhost:8761/eureka/

# Service Ports (defaults: User=8081, Wallet=8082, Payment=8083)
SERVER_PORT=8081  # For user-service
SERVER_PORT=8082  # For wallet-service
SERVER_PORT=8083  # For payment-service

# JWT Configuration (User Service)
JWT_TIMEOUT=900000  # 15 minutes in milliseconds
REFRESH_TOKEN_TIMEOUT=604800000  # 7 days in milliseconds
COOKIE_SECURE_VALUE=false  # Set to true in production with HTTPS
PRIVATE_KEY_PATH=classpath:keys/private.pem
PUBLIC_KEY_PATH=classpath:keys/public.pem
```

### Installation

1. **Clone the repository**

   ```bash
   git clone https://github.com/yourusername/apexpay-distributed-system.git
   cd apexpay-distributed-system
   ```

2. **Set up the database**

   ```bash
   # Create database
   createdb apexpay

   # Run init script (if applicable)
   psql -d apexpay -f user-service/src/main/resources/init.sql
   ```

3. **Build all services**

   ```bash
   mvn clean install
   ```

4. **Start services in order**

   ```bash
   # Terminal 1: Discovery Server (start first, wait for it to be ready)
   cd discovery-server && mvn spring-boot:run

   # Terminal 2: API Gateway
   cd api-gateway && mvn spring-boot:run

   # Terminal 3: User Service
   cd user-service && mvn spring-boot:run

   # Terminal 4: Wallet Service
   cd wallet-service && mvn spring-boot:run

   # Terminal 5: Payment Service
   cd payment-service && mvn spring-boot:run
   ```

5. **Verify services are running**
   - Eureka Dashboard: http://localhost:8761
   - API Gateway Health: http://localhost:9000/actuator/health

## API Documentation

<!-- Option 1: Link to Swagger/OpenAPI -->
<!-- API documentation available at: http://localhost:9000/swagger-ui.html -->

### Authentication Endpoints

All authentication endpoints are prefixed with `/api/v1/auth/` and routed through the API Gateway.

| Method | Endpoint                | Description                              |
| ------ | ----------------------- | ---------------------------------------- |
| POST   | `/api/v1/auth/register` | Register new user                        |
| POST   | `/api/v1/auth/login`    | Authenticate user and receive JWT tokens |
| POST   | `/api/v1/auth/refresh`  | Refresh access token using refresh token |
| POST   | `/api/v1/auth/logout`   | Logout user and revoke refresh token     |

**Note:** JWT tokens are returned as HttpOnly cookies for security.

### Wallet Endpoints

All wallet endpoints are prefixed with `/api/v1/wallet/` and require authentication.

| Method | Endpoint                                 | Description                                |
| ------ | ---------------------------------------- | ------------------------------------------ |
| POST   | `/api/v1/wallet/create`                  | Create a new wallet for authenticated user |
| GET    | `/api/v1/wallet/{walletId}/balance`      | Get wallet balance                         |
| POST   | `/api/v1/wallet/topup`                   | Top up wallet balance                      |
| POST   | `/api/v1/wallet/transfer`                | Transfer funds between wallets             |
| POST   | `/api/v1/wallet/payment`                 | Process payment from wallet                |
| GET    | `/api/v1/wallet/{walletId}/transactions` | Get transaction history                    |

### Payment Endpoints

Payment endpoints are prefixed with `/api/v1/payment/` and require authentication.

| Method | Endpoint                        | Description                                  |
| ------ | ------------------------------- | -------------------------------------------- |
| POST   | `/api/v1/payment`               | Initiate payment with idempotency key        |
| POST   | `/api/v1/payment/{id}/process`  | Process payment (reserve, charge, confirm)   |
| GET    | `/api/v1/payment/{id}/status`   | Check/poll payment status                    |

<!-- Add example requests/responses for key endpoints -->

<details>
<summary>Example: User Registration</summary>

**Request:**

```bash
curl -X POST http://localhost:9000/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "username": "johndoe",
    "password": "securePassword123"
  }'
```

**Response:**

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "email": "user@example.com",
  "username": "johndoe",
  "createdDate": "2025-01-21T10:00:00Z"
}
```

**Note:** After successful registration, you can login to receive JWT tokens stored in HttpOnly cookies.

</details>

## Project Structure

```
apexpay-distributed-system/
├── api-gateway/                 # API Gateway service
│   └── src/main/java/.../
│       ├── config/              # Gateway routes, JWT filter, security
│       ├── controller/          # Fallback controllers
│       └── exception/           # Global error handling
├── common/                      # Shared module
│   └── src/main/java/.../
│       ├── constants/           # Shared constants
│       ├── dto/                 # Shared DTOs
│       ├── enums/               # Shared enums
│       └── exception/           # Common exceptions
├── discovery-server/            # Eureka server
├── payment-service/             # Payment processing
│   └── src/main/java/.../
│       ├── client/provider/     # Payment provider clients
│       ├── controller/          # REST endpoints
│       ├── dto/                 # Request/Response DTOs
│       ├── entity/              # JPA entities
│       ├── enums/               # Payment enums
│       ├── repository/          # Data access layer
│       └── service/             # Business logic
├── user-service/                # User management & auth
│   └── src/main/java/.../
│       ├── controller/          # REST endpoints
│       ├── dto/                 # Request/Response DTOs
│       ├── entity/              # JPA entities
│       ├── repository/          # Data access layer
│       ├── security/            # JWT & Spring Security
│       └── service/             # Business logic
├── wallet-service/              # Wallet management
│   └── src/main/java/.../
│       ├── config/              # Retry configuration
│       ├── controller/          # REST endpoints
│       ├── dto/                 # Request/Response DTOs
│       ├── entity/              # JPA entities
│       ├── enums/               # Wallet enums
│       ├── repository/          # Data access layer
│       └── service/             # Business logic
└── pom.xml                      # Parent POM
```

## Design Decisions

This section explains key architectural and design decisions made during development. These decisions demonstrate understanding of distributed systems principles and trade-offs.

### Why Microservices?

**Decision:** Separate services for User, Wallet, and Payment domains.

**Rationale:**

- **Independent Deployment:** Each service can be deployed, scaled, and updated independently. For example, wallet operations can scale separately from user authentication during peak payment periods.
- **Technology Flexibility:** Each service can evolve its tech stack independently (though we use consistent stack for simplicity).
- **Team Autonomy:** Different teams can own different services with clear boundaries.
- **Fault Isolation:** If the payment service fails, user authentication and wallet queries can still function.
- **Learning Objective:** This project was built to understand microservices communication patterns, service discovery, and API gateway routing.

**Trade-offs Considered:**

- **Complexity:** Microservices add operational complexity (service discovery, distributed tracing, etc.). For a small team, a monolith might be simpler initially.
- **Network Latency:** Inter-service calls add network overhead. We mitigate this with efficient communication patterns and circuit breakers.
- **Data Consistency:** Distributed transactions are harder. We use eventual consistency with optimistic locking for wallet operations.

**When to Use:** Microservices make sense when you have different scaling requirements, need independent deployments, or have multiple teams. For a simple CRUD app, a monolith would be more appropriate.

### Authentication Strategy

**Decision:** JWT with RS256 (public/private key), refresh token rotation, and family-based token revocation.

**Why JWT over Sessions?**

- **Stateless:** Gateway can validate tokens without calling user service, reducing latency and load.
- **Scalability:** No need for sticky sessions or shared session store across instances.
- **Microservices-Friendly:** Services can validate tokens independently using the public key.

**Why RS256 (Public/Private Key) over HS256 (Symmetric)?**

- **Security:** Private key stays in user-service (token issuer). Gateway only needs public key to validate, reducing attack surface.
- **Scalability:** Multiple gateways/services can validate tokens without sharing secrets.
- **Industry Standard:** RS256 is recommended by OAuth 2.0 and JWT best practices.

**Refresh Token Rotation:**

- Each refresh generates a new access token AND a new refresh token.
- Old refresh token is marked as consumed.
- If a consumed token is reused, it indicates a potential attack → revoke entire token family.
- **Family ID:** Groups related tokens for cascade revocation on security events.

**Implementation Details:**

- Access tokens: Short-lived (15 minutes) to minimize exposure if compromised.
- Refresh tokens: Longer-lived (7 days), stored hashed in database, tied to IP address.
- Tokens stored in HttpOnly cookies to prevent XSS attacks.
- SameSite=Strict to prevent CSRF attacks.

**Why Not OAuth2?**

- OAuth2 adds complexity (authorization server, resource server concepts).
- For a single-tenant application, JWT with refresh tokens provides sufficient security.
- OAuth2 would be appropriate for multi-tenant or third-party integrations.

### Database per Service vs Shared Database

**Decision:** Separate PostgreSQL schemas per service within a single database instance.

**Why Separate Schemas?**

- **Logical Separation:** Each service has its own schema (`userservice`, `walletservice`, `paymentservice`), preventing accidental cross-service data access.
- **Future Migration Path:** Easy to split into separate databases later if needed.
- **Data Ownership:** Clear boundaries - wallet service cannot directly query user tables.

**Why Not Separate Databases Initially?**

- **Operational Simplicity:** Single database instance is easier to manage, backup, and monitor during development.
- **Transaction Support:** Cross-service transactions (if needed) are simpler within one database.
- **Resource Efficiency:** Single database instance uses fewer resources than multiple instances.

**Trade-offs:**

- **Shared Infrastructure:** Services share database resources, which could cause contention at scale.
- **Coupling Risk:** Services are still coupled to the same database instance, though schemas provide logical isolation.

**Production Consideration:** In production with high load, we would migrate to separate database instances per service for true isolation and independent scaling.

### Distributed Transactions (Two-Phase Commit)

**Decision:** Use a simplified Two-Phase Commit (2PC) pattern for cross-service payment consistency.

**Rationale:**
- **Data Integrity:** Ensures that we don't charge a user if we can't reserve their funds, and we don't keep funds locked if the charge fails.
- **Phase 1 (Prepare):** Wallet service "reserves" funds. This guarantees the money is available and blocked from other transactions.
- **Phase 2 (Commit/Rollback):** Based on the external provider's response, the payment service instructs the wallet to either "confirm" (permanent deduction) or "cancel" (release back to balance).

**Trade-offs:**
- **Orchestration Overhead:** The Payment Service acts as the orchestrator, increasing its complexity.
- **Zombie Reservations:** If the Payment Service crashes mid-flow, funds might stay reserved. *Future Fix: Implement a reconciliation worker to clean up expired reservations.*

### Mock Payment Provider Strategy

**Decision:** Implement a feature-rich `MockPaymentProviderClient` instead of a simple stub.

**Features:**
- **Deterministic Testing:** Use tokens like `tok_success`, `tok_decline`, or `tok_retry` to trigger specific flows.
- **Reliability Simulation:** Configurable `successRate` and `latency` to test circuit breakers and timeouts.
- **Stateful:** Stores transactions in-memory to support status polling (`getTransactionStatus`).

### Idempotency in Payments

**Decision:** Client-provided `client_request_id` with database UNIQUE constraint.

**Why Idempotency is Critical:**

- Network retries, user double-clicks, or system failures can cause duplicate payment requests.
- Without idempotency, a user could be charged twice for the same payment.

**Implementation:**

```sql
CREATE TABLE paymentservice.payments(
    ...
    client_request_id VARCHAR(255) UNIQUE NOT NULL,
    ...
);
```

**How It Works:**

1. Client generates a unique `client_request_id` (e.g., UUID) for each payment request.
2. Payment service checks if a payment with this `client_request_id` already exists.
3. If exists: Return the existing payment (idempotent response).
4. If not: Process payment and store with `client_request_id`.

**Why Client-Provided ID?**

- **Flexibility:** Client controls retry logic and can use meaningful IDs (e.g., order ID).
- **Simplicity:** No need for server-side token generation or distributed locking.
- **Idempotency Window:** Works across service restarts and database transactions.

**Alternative Considered:**

- **Idempotency Keys in Redis:** Faster lookups but adds infrastructure dependency.
- **Database-only approach:** Simpler, works with existing PostgreSQL setup, sufficient for current scale.

**Future Enhancement:** For high-throughput scenarios, we could add a Redis cache layer for faster idempotency checks while maintaining database as source of truth.

### Error Handling Strategy

**Decision:** Centralized error handling with `ErrorCode` enum and `GlobalExceptionHandler` in each service.

**Why Centralized Error Handling?**

- **Consistency:** All services return errors in the same format.
- **Maintainability:** Error codes and messages defined in one place (`ErrorCode` enum in common module).
- **User Experience:** Consistent error responses help frontend developers handle errors predictably.

**Error Response Format:**

```json
{
  "status": 400,
  "code": 3001,
  "error": "Bad Request",
  "message": "Validation failed: email: must not be blank",
  "path": "/api/v1/auth/register"
}
```

**Error Code Ranges:**

- `1xxx`: Authentication errors (invalid token, expired, etc.)
- `2xxx`: Resource errors (not found, etc.)
- `3xxx`: Validation errors
- `4xxx`: Conflict errors (duplicate email, etc.)
- `5xxx`: Authorization errors (insufficient balance, etc.)
- `9xxx`: Server errors (internal errors, service unavailable)

**Exception Handling Hierarchy:**

1. **BusinessException:** Application-specific errors with `ErrorCode` → Returns structured error response.
2. **ValidationException:** Spring `@Valid` failures → Returns validation error details.
3. **SecurityException:** Authentication/authorization failures → Returns generic message to prevent user enumeration.
4. **Generic Exception:** Unexpected errors → Returns generic 500 error, logs full stack trace.

**Why Generic Messages for Security Errors?**

- Prevents user enumeration attacks (e.g., "User not found" vs "Invalid password" reveals if email exists).
- Security exceptions return generic "Invalid credentials" message.

**Gateway Error Handling:**

- Gateway has its own `GlobalErrorWebExceptionHandler` for reactive WebFlux errors.
- Detects service unavailability and returns appropriate 503 responses.
- Maps downstream service errors to consistent format.

### Circuit Breaker Configuration

**Decision:** Resilience4j circuit breaker with fallback responses in API Gateway.

**Why Circuit Breakers?**

- **Fault Tolerance:** Prevents cascading failures when downstream services are slow or unavailable.
- **Fast Failure:** Returns fallback response immediately instead of waiting for timeout.
- **Resource Protection:** Stops sending requests to failing services, allowing them to recover.

**Configuration:**

```yaml
resilience4j:
  circuitbreaker:
    instances:
      userserviceCB:
        slidingWindowSize: 10 # Last 10 calls
        failureRateThreshold: 50 # Open if 50% fail
        waitDurationInOpenState: 10s # Wait 10s before retry
        permittedNumberOfCallsInHalfOpenState: 3 # Test with 3 calls
```

**How It Works:**

1. **Closed State:** Normal operation, requests flow through.
2. **Open State:** Circuit opens when failure rate exceeds threshold. Requests immediately return fallback.
3. **Half-Open State:** After wait duration, allows limited requests to test if service recovered.
4. **Back to Closed:** If test requests succeed, circuit closes and normal operation resumes.

**Fallback Strategy:**

- Gateway routes have fallback URIs pointing to `FallbackController`.
- Returns user-friendly messages: "The User Service is currently taking too long to respond or is down."
- HTTP 503 (Service Unavailable) status code.

**Why Resilience4j over Hystrix?**

- **Active Maintenance:** Resilience4j is actively developed; Hystrix is in maintenance mode.
- **Functional Style:** Resilience4j uses functional programming, integrates well with Java 8+.
- **Flexibility:** Works with both blocking (Spring MVC) and reactive (WebFlux) code.

**Future Enhancement:**

- Add retry mechanism with exponential backoff.
- Implement bulkhead pattern to isolate thread pools per service.
- Add rate limiting to prevent service overload.

### Distributed Transactions

**Current Approach:** Optimistic locking with retry mechanism.

**Why Not 2PC (Two-Phase Commit)?**

- 2PC is blocking and can cause performance issues.
- Requires a transaction coordinator, adding complexity.
- Not suitable for high-throughput scenarios.

**Why Not Saga Pattern?**

- Saga pattern requires event sourcing or choreography/orchestration.
- Adds complexity with compensation logic.
- Current scale doesn't justify the added complexity.

**Current Implementation:**

- **Optimistic Locking:** Wallet entity uses `@Version` annotation.
- **Retry Mechanism:** `@Retryable` annotation retries up to 3 times on `ObjectOptimisticLockingFailureException`.
- **Eventual Consistency:** Payment and wallet updates are eventually consistent.
- **Compensation:** If payment fails after wallet deduction, wallet service handles rollback.

**Example Flow:**

1. Payment service initiates payment.
2. Wallet service deducts funds (with optimistic lock).
3. If concurrent update conflict, retry with backoff.
4. If payment fails, wallet service can reverse the transaction.

**Future Enhancement:**

- Implement Saga pattern for complex multi-service transactions.
- Add event-driven architecture with message queue (Kafka/RabbitMQ).
- Implement outbox pattern for reliable event publishing.

## Testing

**Current Status:** Testing infrastructure is planned but not yet implemented. Currently, only default Spring Boot context loading tests exist.

**Planned Testing Strategy:**

- **Unit Tests:** Service layer logic with mocked dependencies (JUnit 5, Mockito).
- **Integration Tests:** Repository layer with test database (Testcontainers or H2).
- **API Tests:** Controller endpoints with MockMvc for request/response validation.
- **Contract Tests:** OpenFeign client contracts to ensure service compatibility.

**Target Test Pyramid:**

- **70% Unit Tests:** Fast, isolated tests for business logic.
- **20% Integration Tests:** Database interactions and service integrations.
- **10% End-to-End Tests:** Critical user flows across services.

## Future Improvements

<!-- Show that you're thinking ahead - great for interview discussions -->

- [ ] Docker Compose setup for local development
- [ ] Kubernetes deployment manifests
- [ ] Comprehensive test suite (unit, integration, API, and contract tests)
- [ ] Contract testing with Pact for service contracts
- [ ] Chaos engineering tests with Chaos Monkey
- [ ] Distributed tracing with Zipkin/Jaeger
- [ ] Centralized logging with ELK Stack
- [ ] Message queue integration (Kafka/RabbitMQ) for async processing
- [ ] Caching layer with Redis
- [ ] API rate limiting
- [ ] Comprehensive monitoring with Prometheus/Grafana
- [ ] CI/CD pipeline with GitHub Actions
- [ ] Load testing with Gatling/k6
