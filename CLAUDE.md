# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commit Message Rules

### ❌ DO NOT ADD:
- **NO emojis** in commit messages (unless explicitly requested)
- **NO Claude Code attribution footer** (🤖 Generated with [Claude Code]...)
- **NO Co-Authored-By: Claude** footer

### ✅ DO ADD:
- Conventional commit format (feat:, fix:, refactor:, docs:, etc.)
- Clear, descriptive commit messages
- Implementation details in commit body

### Rules for Creating Great Commit Messages:
1. **Limit subject line to 50 characters**
2. **Capitalize the subject/description line**
3. **Do not end subject line with a period**
4. **Separate subject from body with a blank line**
5. **Wrap body at 72 characters**
6. **Use body to explain what and why** (not how - code shows how)
7. **Use imperative mood in subject** (like giving a command)
    - Good: "Add unit tests for authentication"
    - Bad: "Added unit tests" or "Adds unit tests"

## Example Correct Commit:

```
feat: Implement certificate management for CDP

Add CDP-compliant certificate management for MongoDB and HTTP
clients:

- Scan TRUSTSTORE_* environment variables for custom CAs
- Load base64-encoded PEM certificates with error handling
- Create SSLContext combining JVM defaults with CDP certificates
- Configure MongoDB client with custom SSL for AWS IAM auth
- Configure RestClient and RestTemplate with custom SSL
```

Note: Subject is 47 characters, imperative mood, capitalized, no
period. Body wrapped at 72 characters and explains what/why.

## Essential Commands
### Running Specific Tests
```bash
# Run single test class
mvn test -Dtest=ExampleServiceTest

# Run single test method
mvn test -Dtest=ExampleServiceTest#shouldCreateExample

# Run all integration tests
mvn test -Dtest=*IT

# Run tests with coverage report
mvn test jacoco:report
# View: target/site/jacoco/index.html
```

### Direct Execution (No Docker)
```bash
# Backend only (requires MongoDB running)
mvn spring-boot:run

# With specific profile
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

### Docker Commands
```bash
# View logs
docker compose logs -f trade-demo-backend

# Check service status
docker compose ps

# Access container shell
docker compose exec trade-demo-backend sh

# Rebuild specific service
docker compose up --build trade-demo-backend
```

## Architecture Overview

### Package Structure
```
uk.gov.defra.cdp.trade.demo
├── config/              # Spring configuration beans
│   ├── tls/            # Certificate loading and SSL context
│   ├── MongoConfig     # MongoDB client with IAM auth
│   ├── ProxyConfig     # HTTP proxy configuration
│   ├── EmfMetricsConfig # CloudWatch metrics setup
│   └── RestClientConfig # HTTP client with custom SSL
├── filter/             # Servlet filters for request processing
│   └── RequestTracingFilter # Extract x-cdp-request-id header
├── interceptor/        # HTTP client interceptors
│   └── TraceIdPropagationInterceptor # Add trace ID to outbound calls
├── logging/            # Custom logging components
│   └── HealthCheckFilter # Suppress /health endpoint logs
├── common/
│   ├── error/          # Global exception handling
│   └── metrics/        # MetricsService abstraction (EMF + NoOp)
├── example/            # Example domain (replace with actual features)
│   ├── ExampleController
│   ├── ExampleService
│   ├── ExampleRepository
│   └── ExampleEntity
└── debug/              # Debug endpoints (excluded from coverage)
```

### CDP Compliance Architecture

**Request Flow:**
1. `RequestTracingFilter` (HIGHEST_PRECEDENCE) extracts `x-cdp-request-id` header
2. Populates MDC with trace ID, HTTP method, URL
3. Controller processes request
4. Response status added to MDC
5. ECS encoder writes JSON log with all MDC fields
6. MDC cleared in finally block (prevents thread pool contamination)

**Outbound HTTP Calls:**
1. `TraceIdPropagationInterceptor` reads trace ID from MDC
2. Adds `x-cdp-request-id` header to outbound RestClient/RestTemplate requests
3. Custom SSLContext (from TrustStoreConfiguration) applied to HTTP clients

**Certificate Management:**
1. `TrustStoreConfiguration` creates SSLContext at startup (HIGHEST_PRECEDENCE)
2. `CertificateLoader` scans environment for `TRUSTSTORE_*` variables
3. Base64-encoded PEM certificates loaded from environment
4. `CombinedTrustManager` delegates to both JVM defaults and custom certificates
5. SSLContext injected into MongoConfig and RestClientConfig

**Logging:**
- Standard ECS JSON format with flat `error.*` fields (error.type, error.message, error.stack_trace)
- `HealthCheckFilter` suppresses /health endpoint logs (reduces noise)
- All logs written to stdout in ECS JSON format

**Metrics:**
- `EmfMetricsService` (production) writes AWS Embedded Metrics Format to stdout
- `NoOpMetricsService` (test profile) silent no-op implementation
- Platform auto-injects AWS_EMF_NAMESPACE and CloudWatch credentials
- CloudWatch Agent sidecar processes EMF logs and creates metrics

### Key CDP Integration Points

**MongoDB Connection:**
- `MongoConfig` extends `AbstractMongoClientConfiguration`
- Connection string must include `authMechanism=MONGODB-AWS&authSource=$external`
- Custom SSLContext injected via constructor
- Connection pool settings configured
- `@PreDestroy` ensures graceful shutdown

**HTTP Proxy:**
- `ProxyConfig` reads `HTTP_PROXY` environment variable at startup
- Sets Java system properties (http.proxyHost, http.proxyPort, etc.)
- Configures default ProxySelector for all HTTP clients

**Graceful Shutdown:**
- `server.shutdown=graceful` in application.yml
- 30 second timeout for in-flight requests
- MongoConfig and EmfMetricsConfig have `@PreDestroy` cleanup methods

### Configuration Hierarchy
```
application.yml          # Base configuration (all environments)
application-dev.yml      # Development overrides (additional actuator endpoints)
Environment Variables    # Runtime configuration (takes precedence)
```

All production configuration via environment variables (fail-fast on missing required values).

### Testing Strategy

**Unit Tests:**
- Suffix: `*Test.java`
- No Docker required
- `@WebMvcTest` for controller slices
- MockBean for dependencies

**Integration Tests:**
- Suffix: `*IT.java`
- Uses Testcontainers (real MongoDB instances)
- `@SpringBootTest` with full context
- Requires Docker runtime

**Coverage:**
- Minimum 65% line coverage (enforced by JaCoCo)
- Debug package (`uk.gov.defra.cdp.trade.demo.debug`) excluded from coverage

### Common Patterns

**Adding a New Domain Entity:**
1. Create package under `uk.gov.defra.cdp.trade.demo.<domain>`
2. Add: Entity, Repository (extends MongoRepository), Service, Controller
3. Add validation annotations to Entity (`@NotNull`, `@NotBlank`, etc.)
4. Inject `MetricsService` into Service for custom metrics
5. Write unit tests for Service (mock repository)
6. Write integration tests for Controller (with Testcontainers)

**Logging Patterns:**
```java
// CORRECT - exception object as final parameter
logger.error("Failed to process: {}", id, exception);

// WRONG - loses stack trace in OpenSearch
logger.error("Failed to process: {}. Error: {}", id, exception.getMessage());
```

**Metrics Patterns:**
```java
// Simple counter
metricsService.counter("orders.processed");

// Counter with dimensions (for filtering in CloudWatch)
metricsService.counter("orders.processed", 1.0,
    DimensionSet.of("orderType", order.getType())
);

// Counter with searchable context properties
metricsService.counterWithContext("order.processing.time",
    durationMs,
    DimensionSet.of("orderType", order.getType()),
    Map.of("orderId", order.getId())
);
```

## Important Files

- `CDP-COMPLIANCE.md` - Detailed CDP requirements implementation
- `FINDINGS.md` - Technical analysis of platform integration issues
- `README.md` - Getting started and development workflows
- `Dockerfile` - Multi-stage build with production and test targets
- `docker-compose.yml` - Local development environment
- `pom.xml` - Maven dependencies and build configuration
- `src/main/resources/application.yml` - Base Spring Boot configuration
- `src/main/resources/logback-spring.xml` - ECS JSON logging configuration

## CDP Platform Notes

**Automatic Platform Configuration:**
- Trace ID (`trace.id`) - Platform populates from Data Prepper
- Service name/version - Platform injects from ECS task metadata
- CloudWatch credentials - Platform provides via IAM role
- EMF namespace - Platform sets to service name

**Do NOT set these in application code:**
- `trace.id` in MDC (use `x-cdp-request-id` header for correlation)
- `service.name` / `service.version` in EcsEncoder (platform overrides)
- AWS credentials for CloudWatch (platform provides)

**Platform Requirements:**
- Health checks must NOT test database connectivity (CDP requirement)
- /health must respond in under 5 seconds (Dockerfile HEALTHCHECK enforces)

## Migration Context

This repository was migrated from C# ASP.NET to Java Spring Boot on 2025-10-14. The original .NET code is preserved in the `dotnet-original-code` branch. All GitHub OIDC infrastructure and deployment workflows were preserved during migration.