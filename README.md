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
AWS_EMF_ENABLED=false
AWS_EMF_NAMESPACE=trade-demo-backend
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

## Custom Metrics

This service uses **AWS Embedded Metrics Format (EMF)** for custom business metrics, matching the CDP Node.js and .NET patterns.

### How It Works

EMF writes structured JSON logs that CloudWatch **automatically extracts** into metrics:

```java
@Autowired
private MetricsService metricsService;

public void processOrder(Order order) {
    long startTime = System.currentTimeMillis();

    // Business logic...

    // Record simple counter
    metricsService.counter("orders.processed");

    // Record counter with dimensions for filtering
    metricsService.counter("orders.processed", 1.0,
        DimensionSet.of("orderType", order.getType())
    );

    // Record metric with searchable context properties
    metricsService.counterWithContext("order.processing.time",
        System.currentTimeMillis() - startTime,
        DimensionSet.of("orderType", order.getType()),
        Map.of(
            "orderId", order.getId(),
            "customerId", order.getCustomerId()
        )
    );
}
```

### Benefits

- **No CloudWatch API calls** - Writes logs only, CloudWatch extracts metrics
- **Queryable context** - Properties searchable in CloudWatch Logs Insights
- **High throughput** - Non-blocking, no `"error sending metric data"` failures
- **CDP-compliant** - Matches Node.js and .NET template patterns

### Configuration

**Enable EMF (optional, disabled by default):**
- `AWS_EMF_ENABLED=true`

**When EMF is enabled, these variables are used:**
- `AWS_EMF_NAMESPACE` - CloudWatch namespace (REQUIRED, fails startup if missing)
- `AWS_EMF_SERVICE_NAME` - Service name (optional, default: `trade-demo-backend`)
- `AWS_EMF_SERVICE_TYPE` - Service type (optional, default: `SpringBootApp`)

**Example:**
```bash
AWS_EMF_ENABLED=true
AWS_EMF_NAMESPACE=trade-demo-backend
AWS_EMF_SERVICE_NAME=trade-demo-backend
AWS_EMF_SERVICE_TYPE=SpringBootApp
```

### Viewing Metrics

**CloudWatch Metrics:**
1. Navigate to CloudWatch → Metrics
2. Select namespace: `trade-demo-backend`
3. Metrics appear automatically from logs

**CloudWatch Logs Insights:**
Query detailed context:
```
fields @timestamp, orderId, customerId, order.processing.time
| filter orderId = "12345"
| sort @timestamp desc
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

## Debugging Production Logs

### Accessing OpenSearch Dashboards

CDP logs are available in OpenSearch Dashboards:

- **Dev**: `https://logs.dev.cdp-int.defra.cloud/_dashboards`
- **Test**: `https://logs.test.cdp-int.defra.cloud/_dashboards`
- **Prod**: `https://logs.prod.cdp-int.defra.cloud/_dashboards`

### Verifying Stack Traces

Stack traces only appear in OpenSearch when exceptions are logged correctly. Verify your service logs stack traces properly:

**Check if error.stack_trace field exists:**
```json
GET /cdp-logs-*/_search
{
  "query": {
    "bool": {
      "must": [
        { "term": { "service.name.keyword": "trade-demo-backend" } },
        { "exists": { "field": "error.stack_trace" } }
      ]
    }
  },
  "size": 1,
  "_source": ["error.*", "message", "@timestamp", "service.version"]
}
```

**Expected result if working correctly:**
- `hits.total.value` > 0
- `_source` contains `error.stack_trace`, `error.type`, `error.message`

**If 0 results but errors are being logged:**
Your code is using the WRONG logging pattern. Check that all `logger.error()` calls pass the exception object as the final parameter:

```java
// CORRECT - Stack traces appear in OpenSearch
logger.error("Failed to process: {}", id, exception);

// WRONG - NO stack traces in OpenSearch
logger.error("Failed to process: {}. Error: {}", id, exception.getMessage());
```

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

### Platform Limitation: Java Error Stack Traces (IMPORTANT)

**Issue:** CDP's Data Prepper pipeline filters out Java error stack traces due to a structural incompatibility between Java's `logback-ecs-encoder` (flat fields) and the platform's `select_entries` whitelist (nested objects).

**Root Cause:**
- **Node.js services (Pino)** output: `"error": { "type": "...", "message": "...", "stack_trace": "..." }` (nested object ✅)
- **Java services (Logback)** output: `"error.type": "..."`, `"error.message": "..."`, `"error.stack_trace": "..."` (flat fields ❌)
- **Data Prepper whitelist** uses slash notation (`error/type`, `error/message`, `error/stack_trace`) which matches nested objects but NOT flat fields

**Impact:** Java services using standard `logback-ecs-encoder` will NOT have `error.*` fields in OpenSearch, making error debugging difficult.

**Workaround (Experimental):**

This service includes an **experimental** `NestedErrorEcsEncoder` that transforms flat error.* fields to nested error objects, matching the Node.js format:

```xml
<!-- logback-spring.xml -->
<encoder class="uk.gov.defra.cdp.trade.demo.logging.NestedErrorEcsEncoder">
    <serviceName>${spring.application.name}</serviceName>
    <serviceVersion>${SERVICE_VERSION}</serviceVersion>
</encoder>
```

The custom encoder:
- ✅ Extends standard EcsEncoder (preserves all ECS fields)
- ✅ Transforms error.* flat fields to nested error {} object
- ✅ Performance-optimized with pre-compiled regex patterns
- ✅ Fully tested (9 unit tests, 100% encoder coverage)
- ⚠️ Experimental quality - requires production hardening

**Testing the Workaround:**

Verify the fix works in DEV by running the side-by-side comparison experiment:

```bash
# 1. Deploy to DEV (encoder already configured in separate logger)
# 2. Run experiment endpoint
curl -X POST https://trade-demo-backend.dev.cdp-int.defra.cloud/debug/run-nested-error-experiment

# 3. Query OpenSearch for results
GET /cdp-logs-*/_search
{
  "query": { "match": { "message": "NESTED_EXP" } },
  "_source": ["message", "error.*", "error", "log.level", "@timestamp"]
}

# 4. Compare results:
# - [FLAT FORMAT] logs: No error.stack_trace (filtered by Data Prepper) ❌
# - [NESTED FORMAT] logs: Has error.stack_trace (passes through Data Prepper) ✅
```

**Long-term Solutions:**

1. **Platform Fix (Recommended):** CDP platform team adds `rename_keys` processor to Data Prepper pipeline to handle flat dotted fields
2. **Application Fix:** Production-harden the custom encoder if platform fix delayed
3. **Upstream Fix:** Request `logback-ecs-encoder` library to support nested error objects

**See Also:** [FINDINGS.md](FINDINGS.md) for complete technical analysis and empirical proof.

---

### Common Issues

**No error.* fields in logs:**
- **Java-specific platform limitation:** See "Platform Limitation: Java Error Stack Traces" above
- Code is logging `exception.getMessage()` instead of `exception` object
- Review all `catch` blocks and verify `logger.error()` calls
- See ECS logging section in `.claude/agents/java-code-researcher.md`

**Field mapping exists but no data:**
- Verify deployed version includes correct logging code
- Check service.version in logs matches expected deployment

**Logs not appearing:**
- Check ECS task is running: `aws ecs list-tasks --cluster <cluster> --service-name trade-demo-backend`
- Verify CloudWatch log group exists: `/aws/ecs/trade-demo-backend`
- Check Data Prepper is processing logs (CDP platform issue - contact support)

---

## References
- [MIGRATION_PLAN.md](MIGRATION_PLAN.md)
- **Spring Boot 3.2:** https://docs.spring.io/spring-boot/docs/3.2.x/reference/html/
- **GitHub Actions setup-java:** https://github.com/actions/setup-java
- [Spring Boot Actuator Metrics](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html#actuator.metrics)
- [AWS Embedded Metrics Java](https://github.com/awslabs/aws-embedded-metrics-java)
- [CDP Custom Metrics Documentation](../../cdp-documentation/how-to/custom-metrics.md)
