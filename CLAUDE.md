# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Development Commands

### Build and Test
```bash
# Build project
mvn clean install

# Run tests
mvn clean test                     # Unit tests only (mvn test)
mvn clean verify -P integration    # All tests including integration tests (mvn clean verify)

# Clean build artifacts and test containers
make clean
```

### Running the Application
```bash
# Full stack with Docker (default)
make start                    # Backend + MongoDB + LocalStack

# Native development (fastest iteration)
mvn spring-boot:run

# Stop all services
make stop
```

### Development Workflow
- **Active development**: Use native mode (`mvn spring-boot:run`) with Docker infrastructure only (`docker compose up mongodb localstack`)
- **Frontend integration**: Use `make start` for full Docker stack
- **Testing**: Always run `make verify` before commits to ensure all tests pass including integration tests

### Docker Operations
```bash
make build-image              # Build Docker image for frontend integration
make logs                     # View logs from all services
make ps                       # Show service status
```

## Architecture Overview

### Core Structure
- **Main Application**: `uk.gov.defra.cdp.trade.demo.Application` - Standard Spring Boot main class
- **Example Service**: CRUD operations with MongoDB (`example/` package)
- **CDP Compliance**: Configuration, logging, tracing, metrics, TLS (`config/`, `common/`, `filter/`, `interceptor/`)
- **Testing**: Testcontainers-based integration tests with MongoDB

### Key Configuration Files
- `application.yml` - Main configuration with CDP environment variables
- `application-dev.yml` - Development profile with exposed metrics/debugging endpoints
- `logback-spring.xml` - ECS JSON logging configuration
- `pom.xml` - Maven dependencies including Spring Boot 3.5, MongoDB, Testcontainers

### CDP Compliance Features
- **ECS Logging**: Structured JSON logs with trace ID propagation
- **Custom Metrics**: AWS EMF (Embedded Metrics Format) via `MetricsService`
- **TLS Management**: Custom CA certificate loading from environment variables
- **Request Tracing**: `x-cdp-request-id` header propagation
- **Proxy Support**: HTTP proxy configuration for outbound requests

## Testing Strategy

### Test Structure
- **Unit Tests**: Standard JUnit 5 tests in `src/test/java`
- **Integration Tests**: Testcontainers with real MongoDB instances
- **Coverage**: JaCoCo with 65% minimum line coverage requirement

### Testcontainers Configuration
- Uses MongoDB Testcontainers for integration tests
- `TESTCONTAINERS_RYUK_DISABLED=true` set in `maven-surefire-plugin` for compatibility
- Requires Docker to be running for `make verify`

### Important Test Patterns
- Metrics are disabled in test profile (uses `NoOpMetricsService`)
- Integration tests verify CDP compliance (logging, tracing, configuration)
- Custom ECS encoder testing validates log format compatibility

## Environment Variables

### Required
```bash
PORT=8085
MONGO_URI=mongodb://localhost:27017
MONGO_DATABASE=trade-demo-backend
SERVICE_VERSION=0.0.0-local
ENVIRONMENT=local
```

### Optional CDP Features
```bash
HTTP_PROXY=http://localhost:3128          # Outbound proxy
AWS_EMF_ENABLED=true                       # Enable custom metrics
AWS_EMF_NAMESPACE=trade-demo-backend       # CloudWatch namespace
TRACING_HEADER=x-cdp-request-id           # Request tracing header
```

## Key Files to Understand

### Core Business Logic
- `example/ExampleController.java` - REST API endpoints
- `example/ExampleService.java` - Business logic with metrics integration
- `example/ExampleEntity.java` - MongoDB document model

### CDP Infrastructure
- `config/CdpConfig.java` - Main CDP configuration
- `common/metrics/MetricsService.java` - Custom CloudWatch metrics interface
- `filter/RequestTracingFilter.java` - Trace ID handling
- `logging/NestedErrorEcsEncoder.java` - Custom ECS encoder for error stack traces

### Configuration Management
- `config/EmfMetricsConfig.java` - AWS EMF metrics setup
- `config/tls/TrustStoreConfiguration.java` - Custom CA certificate loading
- `config/ProxyConfig.java` - HTTP proxy configuration

## Development Notes

### Java Version
- Requires Java 21 with `JAVA_HOME` set
- Use `source .envrc` to configure environment

### Frontend Integration
- Backend runs on port 8085
- Frontend expects endpoints: `/example` (CRUD operations)
- Trace ID propagation via `x-cdp-request-id` header

### Metrics Development
- Use `MetricsService` for custom business metrics
- EMF writes structured logs that CloudWatch extracts as metrics
- Test environment uses no-op implementation

### Error Handling and Logging
- All exceptions should be logged with full stack traces: `logger.error("message", exception)`
- Uses custom ECS encoder to ensure error fields appear in OpenSearch
- Never log `exception.getMessage()` only - always pass the exception object