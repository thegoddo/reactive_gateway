# Reactive API Gateway and Resilient Microservices Architecture

This repository contains a high-performance, asynchronous, non-blocking API Gateway and microservices ecosystem built using Spring Boot 4.x, Spring Cloud Gateway v5, and Spring WebFlux. The system is designed to handle high-throughput edge traffic with integrated security, distributed rate limiting, and fault isolation.

## Architectural Overview

The architecture implements a centralized edge proxy pattern to abstract downstream microservices, consolidate cross-cutting concerns, and enforce resilience boundaries before requests reach internal networks.

```
                    [ Client Requests ]

                             │
                             ▼
                ┌─────────────────────────┐
                │   API Gateway (8080)    │
                ├─────────────────────────┤
                │ - JWT Auth Filter       │ ◀─── [ Redis Container (6379) ]
                │ - Redis Rate Limiter    │      (Distributed Rate Limiting)
                │ - Resilience4j CB       │
                └─────────────────────────┘
                 /                       \
                /                         \
               ▼                           ▼
    ┌────────────────────┐       ┌────────────────────┐
    │ User Service (8081)│       │Order Service (8082)│
    ├────────────────────┤       ├────────────────────┤
    │ - Secure Endpoints │       │ - Public Endpoints │
    └────────────────────┘       └────────────────────┘
```

Key Components

- API Gateway (Port 8080): Serves as the single entry point. It utilizes Netty as its underlying high-performance, non-blocking web server, managing request routing dynamically via reactive stream pipelines.
- User Service (Port 8081): An isolated downstream microservice managing secure resources, requiring authenticated context propagated from the gateway.
- Order Service (Port 8082): A public-facing microservice handling transactional and order data queries.
- Redis (Port 6379): Acts as the centralized cache and state coordinator for managing distributed rate-limiting tokens across multiple gateway nodes.

## Technical Specifications and Design Patterns

1. Asynchronous and Non-Blocking Thread ModelUnlike traditional Servlet-based architectures (Thread-per-Request), this ecosystem runs on the Reactor Netty engine. Requests are processed as asynchronous data streams handled by a small, fixed number of Thread EventLoops. This eliminates the CPU overhead of thread context switching and allows the gateway to handle thousands of concurrent connections with minimal memory footprints.
2. Distributed Rate Limiting (Token Bucket Algorithm)The edge proxy enforces rate-limiting boundaries at the entry layer using Redis. It implements the Token Bucket algorithm to calculate remaining capacity on each transaction non-blockingly.Let $r$ represent the token replenishment rate (tokens/second) and $B$ represent the maximum burst capacity. The available token count $T(t)$ at any time $t$ since the last request at $t_0$ is evaluated mathematically as:$$T(t) = \min\left(B, T(t_0) + r \cdot (t - t_0)\right)$$Each incoming request from a unique IP address decrements $T(t)$ by $1$. If $T(t) < 1$, the gateway immediately returns HTTP Status 429 Too Many Requests, protecting the internal services from denial-of-service (DoS) conditions.
3. Circuit Breaker and Fault Isolation (Resilience4j)To prevent localized microservice failures from cascading and exhausting resource pools, the user_service_route is wrapped in a Resilience4j Circuit Breaker.Sliding Window Size: 20 requests.Failure Rate Threshold: 50%.Wait Duration in Open State: 15,000 milliseconds (15 seconds).If the downstream service experiences latency or crashes, the circuit transitions from CLOSED to OPEN. Subsequent requests bypass the failing service entirely and route to a fallback handler, returning a graceful degradation response.
4. Custom JWT Decryption and Downstream Header MutationThe security layer intercepts incoming requests to authenticated endpoints.
5. Extraction: Extracts the HTTP Authorization header and validates the structural format of the Bearer token.
6. Verification: Validates the cryptographic signature against the secret keys.
7. Mutation: Mutates the incoming reactive ServerHttpRequest dynamically, injecting validated claims into downstream headers (e.g., X-Authenticated-User-Role: USER), allowing downstream microservices to remain stateless and security-agnostic.

## Repository Structure.

```
├── ApiGateway/
│   ├── pom.xml
│   ├── compose.yml
│   └── src/
│       ├── main/
│       │   ├── java/com/learn/apigateway/
│       │   │   ├── ApiGatewayApplication.java
│       │   │   ├── config/RateLimiterConfig.java
│       │   │   ├── controller/FallbackController.java
│       │   │   └── filter/JwtAuthenticationFilter.java
│       │   └── resources/application.yml
│       └── test/java/com/learn/apigateway/ApiGatewayApplicationTests.java
│
├── UserService/
│   ├── pom.xml
│   └── src/
│       ├── main/
│       │   ├── java/com/learn/userservice/
│       │   │   ├── UserServiceApplication.java
│       │   │   └── controller/UserController.java
│       │   └── resources/application.yaml
│       └── test/java/com/learn/userservice/UserControllerTests.java
│
└── OrderService/
    ├── pom.xml
    └── src/
        ├── main/
        │   ├── java/com/learn/orderservice/
        │   │   ├── OrderServiceApplication.java
        │   │   └── controller/OrderController.java
        │   └── resources/application.yaml
        └── test/java/com/learn/orderservice/OrderControllerTests.java
```

## Installation and Execution Guide

### Prerequisites

- Java Development Kit (JDK) 17 or higherApache
- Maven 3.8+
- Docker and Docker Compose

### Step 1: Start the Infrastructure Layer

Run the Redis container required for distributed rate-limiting state management:

```
cd ApiGateway
docker compose up -d
```

### Step 2: Build and Run Downstream Services

Open separate terminal sessions for each service, navigate to their root directories, and execute:

- User Service (Port 8081):cd UserService
  `./mvnw spring-boot:run`
- Order Service (Port 8082):cd OrderService
  `./mvnw spring-boot:run`

### Step 3: Build and Run the API Gatewaycd ApiGateway

```
./mvnw spring-boot:run
```

## API Validation and Integration Testing

1. Public Order Endpoint IntegrationExecute a public GET request through the gateway proxy:

```
curl -i http://localhost:8080/api/v1/orders/history
```

- Expected Response: `200 OK`
- Payload: JSON block from the order microservice database emulation.

2. Authenticated User Profile Endpoint (Valid Token)Provide the valid bearer signature payload to verify authorization mapping:

```
curl -i -H "Authorization: Bearer dev-token-secret" http://localhost:8080/api/v1/users/profile
```

- Expected Response: `200 OK`
- Payload: Contains matching authenticated role metadata mutated dynamically by the edge proxy filter.

3. Authenticated User Profile Endpoint (Invalid/No Token)Verify edge protection mechanisms against missing authorization headers:
   ```
   curl -i http://localhost:8080/api/v1/users/profile
   ```

Expected Response: `401 Unauthorized`
Header Verification: Header metadata includes `X-Gateway-Failure-Reason: Missing Authorization` Header. 4. Rate-Limiter Stress Testing
Validate the token bucket threshold constraints by simulating concurrent transactions:

```
for i in {1..25}; do curl -o /dev/null -s -w "%{http_code}\n" http://localhost:8080/api/v1/orders/history; done
```

- Expected Response: The first 20 operations return 200. Subsequent calls exceeding the burst capacity limits yield 429 (Too Many Requests) until the token replenishes.

1. Circuit Breaker Resilience TestS
   imulate a catastrophic downstream outage on the UserService while maintaining gateway operational availability.
1. Terminate the `UserService` process (port 8081).
1. Issue a request through the secure gateway proxy path:
   `curl -i -H "Authorization: Bearer dev-token-secret" http://localhost:8080/api/v1/users/profile`
   Expected Response: `200 OK` (Fallback Route Handled)

Payload:

```

{
"status": "Service Degraded",
"message": "The downstream user microservice is taking too long to respond. Circuit Breaker is active.",
"fallbackActive": true
}

```

Unit and Integration Testing Suite
Each microservice contains robust integration tests driven by WebTestClient. This allows testing the asynchronous pipelines without blocking resources or relying on actual active downstream microservice deployments during continuous integration (CI) workflows.
Run Automated Tests
Navigate to the root of any microservice directory or the API Gateway directory and execute the test goal:

```

./mvnw clean test

```

- Gateway Integration Tests: Tests JWT extraction, failure paths, and mocks out the reactive Redis infrastructure using `@MockBean` to ensure isolated build safety.
- Microservice Tests: Validate JSON mapping payloads, state propagation, and reactive path parameters.
