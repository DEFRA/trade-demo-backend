# Trade Demo Backend

Spring Boot 3.2.11 backend for the Core Delivery Platform (CDP)

---

> **Migration Note:** This repository was migrated from C# ASP.NET. 
> The original .NET code is preserved in the `dotnet-original-code` branch

---

## What is this?

A CDP-compliant Spring Boot backend that demonstrates:
- Full CRUD operations with MongoDB
- ECS JSON logging with trace ID propagation
- HTTP proxy configuration for outbound requests
- Actuator endpoints with production security defaults

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
make mongo      # Starts backend (Docker) + frontend (native)

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

# Identify any cloudwatch logged metrics
docker exec -it trade-demo-backend-localstack-1 /bin/bash
awslocal cloudwatch list-metrics

```

### Standard Metrics

Micrometer automatically collects standard metrics via Spring Boot Actuator:
- JVM metrics (memory, threads, GC)
- HTTP metrics (request counts, durations)
- Database metrics (connection pool, query times)

**View metrics:**
- **Development**: `/metrics` (when using dev profile: `--spring.profiles.active=dev`)
- **Production**: Metrics endpoint not exposed (security by default)

### Testing

Metrics are **disabled in test profile** using `NoOpMetricsService`.
No mocking required - inject `MetricsService` and call normally:

```java
@Autowired
private MetricsService metricsService;

@Test
void testOrderProcessing() {
    metricsService.counter("test.metric");  // Silent no-op in tests
}
```

---

### Querying Logs in Grafana

If your organization uses Grafana with OpenSearch datasource:

**Find errors with stack traces:**
```lucene
service.name:"trade-demo-backend" AND log.level:"ERROR" AND _exists_:error.stack_trace
```

**Find all errors (to verify service is logging):**
```lucene
service.name:"trade-demo-backend" AND log.level:"ERROR"
```

**View specific error types:**
```lucene
service.name:"trade-demo-backend" AND error.type:"java.lang.IllegalArgumentException"
```

### Verifying Field Mappings

Check which error fields are available in OpenSearch:

```json
GET /cdp-logs-*/_mapping/field/error.*
```

This shows all `error.*` field mappings. The `error.stack_trace` field should be type `text` with a `keyword` subfield:

```json
{
  "cdp-logs-2025.10.18": {
    "mappings": {
      "error.stack_trace": {
        "full_name": "error.stack_trace",
        "mapping": {
          "stack_trace": {
            "type": "text",
            "fields": {
              "keyword": {
                "type": "keyword",
                "ignore_above": 256
              }
            }
          }
        }
      }
    }
  }
}
```

---

## Available Experiments

The service exposes two types of endpoints: production-ready CRUD operations and temporary debug experiments for verifying CDP compliance.

### Example API (Production CRUD Operations)

Standard REST API demonstrating CDP-compliant CRUD operations with MongoDB. All endpoints support trace ID propagation via `x-cdp-request-id` header.

**Create an example:**
```bash
curl -X POST http://localhost:8085/example \
  -H "Content-Type: application/json" \
  -H "x-cdp-request-id: test-trace-123" \
  -d '{"name": "test-example", "value": "test-value"}'
```

**List all examples:**
```bash
curl http://localhost:8085/example \
  -H "x-cdp-request-id: test-trace-123"
```

**Get example by ID:**
```bash
curl http://localhost:8085/example/{id} \
  -H "x-cdp-request-id: test-trace-123"
```

**Update example:**
```bash
curl -X PUT http://localhost:8085/example/{id} \
  -H "Content-Type: application/json" \
  -H "x-cdp-request-id: test-trace-123" \
  -d '{"name": "updated-name", "value": "updated-value"}'
```

**Delete example:**
```bash
curl -X DELETE http://localhost:8085/example/{id} \
  -H "x-cdp-request-id: test-trace-123"
```

### Debug Experiments (CDP Compliance Verification)

```bash
curl -X POST http://localhost:8085/debug/run-metrics-experiments \
  -H "x-cdp-request-id: test-trace-123"
```

Returns: Status, metric count, EMF namespace, and CloudWatch verification path

**Get debug info:**

Returns current service configuration including service name/version, environment, EMF enabled status, namespace, and logging encoder type.

```bash
curl http://localhost:8085/debug/info \
  -H "x-cdp-request-id: test-trace-123"
```

Returns: Current service configuration for troubleshooting

**Note:** Debug endpoints emit structured ECS JSON logs with trace IDs that can be queried in OpenSearch Dashboards or CloudWatch Logs Insights.
---