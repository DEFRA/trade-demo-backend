# Trade Demo Backend

Spring Boot 3.2.11 backend for the Core Delivery Platform (CDP). Demonstrates CDP compliance patterns with modern Java idioms.

**Stack:** Java 21 • Spring Boot 3.2.11 • Maven • MongoDB

---

> **Migration Note:** This repository was migrated from C# ASP.NET to Java Spring Boot on 2025-10-14. The original .NET code is preserved in the `dotnet-original-code` branch. All GitHub OIDC infrastructure and deployment workflows were preserved. The migration maintains 100% CDP compliance with 22/22 critical requirements met.

---

## What is this?

A CDP-compliant Spring Boot backend that demonstrates:
- Full CRUD operations with MongoDB
- ECS JSON logging with trace ID propagation
- TLS/SSL certificate management for CDP internal services
- HTTP proxy configuration for outbound requests
- CloudWatch custom metrics integration
- Actuator endpoints with production security defaults

### Compliance Status

✅ **DEPLOYMENT READY** - Verified by cdp-compliance-reviewer agent (2025-10-14)

- **22/22 Critical Requirements Met** (100%)
- **5/5 Important Requirements Met** (100%)
- **3/3 Testing Requirements Met** (100%)
- **42 Tests Passing, 0 Failures**
- **66% Code Coverage** (exceeds 65% minimum)
- **GitHub Actions Workflows Configured**

All mandatory CDP platform requirements verified. Ready for production deployment.

---

## Quick Start

```bash
# 1. Install JDK 21 (see below for options)
# 2. Set JAVA_HOME (source .envrc or export manually)
make start               # Start backend + MongoDB + LocalStack
```

Verify: `curl http://localhost:8085/health`

---

## Development

### Commands

```bash
make help              # Show all commands
make start             # Run backend with MongoDB and LocalStack (default)
make stop              # Stop all services and remove volumes
make build             # Build project
make build-image       # Build Docker image (for use with frontend)
make test              # Run unit tests (requires JAVA_HOME)
make verify            # Run all tests including integration tests (requires Docker)
make clean             # Clean Maven build and remove test containers
make logs              # Show logs from all running services
make ps                # Show status of all services
```


**Direct execution (no Docker):**
```bash
source .envrc && mvn spring-boot:run    # Start
Ctrl+C                                   # Stop
```

### Running Tests with Testcontainers

Tests use Testcontainers to spin up real MongoDB instances. Configuration differs by Docker runtime:

**Docker Desktop:** Works out of the box. No configuration needed.

**Rancher Desktop:** Requires one-time configuration in `~/.testcontainers.properties`:

```properties
# Use Unix socket client provider
docker.client.strategy=org.testcontainers.dockerclient.UnixSocketClientProviderStrategy

# Point to Rancher Desktop socket
docker.host=unix:///Users/$USER/.rd/docker.sock
```

Then run tests normally:
```bash
source .envrc && mvn test
```

**Why is `TESTCONTAINERS_RYUK_DISABLED=true` set in `pom.xml`?**

The Ryuk sidecar container (used for cleanup) fails to start on some environments including Rancher Desktop. This is a known issue ([testcontainers/testcontainers-java#4166](https://github.com/testcontainers/testcontainers-java/issues/4166), [rancher-desktop#1209](https://github.com/rancher-sandbox/rancher-desktop/issues/1209)). Disabling Ryuk allows tests to run but requires manual cleanup. Use `make clean` to remove orphaned test containers and Maven build artifacts.

The environment variable is set in `maven-surefire-plugin` configuration so it applies automatically to all test runs.

### Developing with Frontend

The frontend (`../trade-demo-frontend`) communicates directly with this backend. Choose the workflow that best fits your needs:

#### Option 1: Native Backend (Fastest for Active Development)

Best when actively developing backend code. Spring Boot hot-reloads many changes without restart.

```bash
# Terminal 1: Infrastructure only
cd ../trade-demo-backend
docker compose up mongodb localstack

# Terminal 2: Backend (native with hot reload)
source .envrc && mvn spring-boot:run

# Terminal 3: Frontend (native)
cd ../trade-demo-frontend
npm run dev
```

**Pros:** Fast iteration, Spring Boot DevTools hot reload, easy debugging
**Cons:** Need to manage multiple terminals

#### Option 2: Full Docker Stack (Production-like)

Best when testing full Docker environment or handing off to frontend team.

```bash
# Terminal 1: Start everything via frontend Makefile
cd ../trade-demo-frontend
make dev        # Starts backend (Docker) + frontend (native)

# After backend code changes:
cd ../trade-demo-backend
make build-image        # Rebuild image
cd ../trade-demo-frontend
docker compose restart trade-demo-backend
```

**Pros:** Production-like environment, clean separation
**Cons:** Slow iteration (~30-60s per rebuild)

#### Option 3: Backend Only in Docker

When you need to test backend Docker image specifically.

```bash
# Terminal 1: Backend in Docker
cd ../trade-demo-backend
make start

# After code changes:
make stop && make start

# Terminal 2: Frontend (native)
cd ../trade-demo-frontend
npm run dev
```

**Pros:** Tests actual Docker image
**Cons:** Slowest iteration cycle

**Backend endpoints used by frontend:**
- `GET /example` - List all examples
- `POST /example` - Create example
- `GET /example/{id}` - Get by ID
- `PUT /example/{id}` - Update example
- `DELETE /example/{id}` - Delete example

The frontend propagates `x-cdp-request-id` headers for distributed tracing.


---

## Architecture

### CDP Compliance

- **Logging** - ECS JSON format with required fields (log.level, trace.id, service.version, http.*, url.full)
- **Tracing** - Propagates `x-cdp-request-id` header across requests
- **Certificates** - Loads custom CAs from `TRUSTSTORE_*` environment variables
- **Proxy** - Routes outbound HTTP/HTTPS through `HTTP_PROXY` (CDP auto-configured)
- **Metrics** - Emits custom CloudWatch metrics (production only)
- **Database** - MongoDB with TLS/SSL and IAM authentication support

### Configuration
Externalized via environment variables following CDP patterns:

```bash
# Required
PORT=8085
MONGO_URI=mongodb://localhost:27017
MONGO_DATABASE=trade-demo-backend
SERVICE_VERSION=0.0.0-local
ENVIRONMENT=local

# Optional
LOG_LEVEL=INFO
ACTUATOR_ENDPOINTS=health
ENABLE_METRICS=false
HTTP_PROXY=http://localhost:3128
```

See `application.yml` for all configuration options.

---

### Useful Docker Commands

```bash
# View logs for specific service
docker compose logs -f trade-demo-backend

# Rebuild specific service
docker compose up --build trade-demo-backend

# Access container shell
docker compose exec trade-demo-backend sh

# Remove everything including volumes
docker compose down -v

# Check service health
docker compose ps
```


## Important Note: Security Headers

**HTTP security headers (HSTS, X-Frame-Options, X-Content-Type-Options, X-XSS-Protection) are NOT included in this implementation and are NOT required by CDP.**

Research findings (2025-10-14):
- **CDP Documentation:** No mention of security headers in platform requirements
- **Template Analysis:** Only 1 of 3 backend templates (Node.js) implements security headers
  - Python backend: ❌ No security headers
  - .NET backend: ❌ No security headers
  - Node.js backend: ✅ Has security headers (Hapi.js framework convenience)
- **Context:** Security headers are frontend/browser concerns, not backend API requirements
- **Production Evidence:** Python and .NET services run successfully in CDP without security headers

An AI compliance agent initially flagged security headers as a "critical blocker," leading to their implementation. However, systematic research of CDP documentation and all three backend templates revealed this was a false positive. Security headers were subsequently removed from this codebase.

**Lesson:** When AI agents flag requirements, verify against official documentation and cross-check ALL reference implementations before implementing. See `IMPLEMENTATION_PLAN.md` section 2.9 for detailed rationale.

---

# Metrics Debugging Guide

## Overview

This guide explains how to debug CloudWatch metrics configuration in trade-demo-backend.

## Quick Start

### Enable Metrics Debugging Locally

Add to your local `.envrc` or run before starting the app:

```bash
export ENABLE_METRICS=true
export CDP_METRICS_DEBUG=true
export AWS_REGION=eu-west-2
export LOGGING_LEVEL_IO_MICROMETER_CLOUDWATCH2=DEBUG
export LOGGING_LEVEL_UK_GOV_DEFRA_CDP_TRADE_DEMO_COMMON_METRICS=DEBUG
```

Then start the application and check logs for:
- `=== MeterRegistry Debug Information ===`
- `CloudWatchMeterRegistry detected`
- Debug logs from `io.micrometer.cloudwatch2`

### Enable Metrics Debugging in CDP

Add to `cdp-app-config/services/trade-demo-backend/dev/trade-demo-backend.env`:

```bash
CDP_METRICS_DEBUG=true
```

Then check CloudWatch Logs for the debug output.

## Configuration Variables

### Required for Metrics

| Variable | Purpose | CDP Provides | Local Default |
|----------|---------|--------------|---------------|
| `ENABLE_METRICS` | Enables metrics collection and export | ✓ (in .env files) | `false` |
| `AWS_REGION` | AWS region for CloudWatch | ✓ (ECS env) | `eu-west-2` |
| AWS Credentials | IAM role for CloudWatch PutMetricData API | ✓ (ECS task role) | ❌ Not available locally |

### Optional for Debugging

| Variable | Purpose | Default |
|----------|---------|---------|
| `CDP_METRICS_DEBUG` | Enables detailed MeterRegistry logging | `false` |
| `LOGGING_LEVEL_IO_MICROMETER_CLOUDWATCH2` | CloudWatch exporter debug logs | `WARN` |
| `LOGGING_LEVEL_UK_GOV_DEFRA_CDP_TRADE_DEMO_COMMON_METRICS` | MetricsService debug logs | `INFO` |

**Note on Environment Variable Naming:**

Spring Boot uses relaxed binding to map environment variables to properties. The pattern:
```
LOGGING_LEVEL_<PACKAGE_NAME_WITH_UNDERSCORES>=<LEVEL>
```

Maps to:
```
logging.level.<package.name.with.dots>=<LEVEL>
```

Example:
- `LOGGING_LEVEL_IO_MICROMETER_CLOUDWATCH2=DEBUG` → `logging.level.io.micrometer.cloudwatch2=DEBUG`
- `LOGGING_LEVEL_UK_GOV_DEFRA_CDP_TRADE_DEMO_COMMON_METRICS=DEBUG` → `logging.level.uk.gov.defra.cdp.trade.demo.common.metrics=DEBUG`

**Limitation:** Environment variables only work for package-level logging, not individual classes (due to lowercase conversion).

## What Gets Logged

### On Startup (when CDP_METRICS_DEBUG=true)

```
=== MeterRegistry Debug Information ===
MeterRegistry class: CompositeMeterRegistry
CompositeMeterRegistry contains 2 registries:
  - Registry: SimpleMeterRegistry
  - Registry: CloudWatchMeterRegistry
    ✓ CloudWatchMeterRegistry detected
    Initial metrics count: 0
Total metrics in registry: 0
=== End MeterRegistry Debug ===
```

### When Recording Metrics (DEBUG level enabled)

```
MetricsService initialized (enabled: true)
Recorded counter metric: orders_created = 5.0
```

### CloudWatch Exporter Publishing (DEBUG level enabled)

```
Publishing 3 metrics to CloudWatch in namespace trade-demo-backend
Successfully published metrics to CloudWatch
```

## Integration Tests

### MetricsServiceEnabledIT

Tests CloudWatch metrics configuration with metrics ENABLED:

```bash
mvn test -Dtest=MetricsServiceEnabledIT
```

This verifies:
- CloudWatchMeterRegistry is created (not SimpleMeterRegistry)
- Metrics are registered in CloudWatchMeterRegistry
- Configuration beans are properly wired

### MetricsServiceIT

Tests MetricsService behavior with metrics DISABLED (default):

```bash
mvn test -Dtest=MetricsServiceIT
```

This verifies:
- Metrics are skipped when ENABLE_METRICS=false
- Service handles errors gracefully
- No metrics registered when disabled

## Spring Boot Auto-Configuration

### CloudWatch Exporter Conditions

Spring Boot creates `CloudWatchMeterRegistry` when:
- Dependency `micrometer-registry-cloudwatch2` is on classpath ✓
- Property `management.metrics.export.cloudwatch.enabled=true` ✓
- Property `management.metrics.export.cloudwatch.namespace` is set ✓
- AWS SDK can create `CloudWatchAsyncClient` ✓

### Verification

Check if auto-configuration ran:
```bash
# Enable Spring Boot auto-configuration debug
export LOGGING_LEVEL_ORG_SPRINGFRAMEWORK_BOOT_AUTOCONFIGURE=DEBUG
```
## References
- [MIGRATION_PLAN.md](MIGRATION_PLAN.md)
- **Spring Boot 3.2:** https://docs.spring.io/spring-boot/docs/3.2.x/reference/html/
- **GitHub Actions setup-java:** https://github.com/actions/setup-java
- [Spring Boot Actuator Metrics](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html#actuator.metrics)
- [Micrometer CloudWatch](https://micrometer.io/docs/registry/cloudwatch)
- [AWS SDK Credential Provider Chain](https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/credentials.html#credentials-chain)
- [CDP Custom Metrics Documentation](../../cdp-documentation/how-to/custom-metrics.md)
