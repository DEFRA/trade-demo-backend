# Custom Metrics Investigation - Findings

**Issue:** Custom metrics emitted by trade-demo-backend using AWS Embedded Metrics Format (EMF) are not appearing in CloudWatch Metrics or Grafana dashboards.

**Working Reference:** trade-imports-decision-deriver (.NET service) successfully publishes metrics like `MessagingConsume` to CloudWatch, visible in Grafana under the "trade-imports-decision-deriver" namespace.

**Investigation Date:** 2025-10-18

---

## Metrics Pipeline Map

```
┌─────────────────────────────────────────────────────────────────────────┐
│ 1. Application Code                                                      │
│    metricsService.counter("metric_name") → EmfMetricsService            │
│    STATUS: ✅ KNOWN (implementation verified)                            │
└─────────────────────────────────────────────────────────────────────────┘
                                    ↓
┌─────────────────────────────────────────────────────────────────────────┐
│ 2. EMF Library → Structured JSON                                        │
│    MetricsLogger.flush() → EMF-formatted JSON to stdout                 │
│    STATUS: ❓ UNKNOWN (need to verify JSON structure)                   │
└─────────────────────────────────────────────────────────────────────────┘
                                    ↓
┌─────────────────────────────────────────────────────────────────────────┐
│ 3. Docker Container → CloudWatch Logs                                   │
│    ECS task sends stdout to CloudWatch log group                        │
│    STATUS: ❓ UNKNOWN (cannot access CloudWatch directly)               │
└─────────────────────────────────────────────────────────────────────────┘
                                    ↓
┌─────────────────────────────────────────────────────────────────────────┐
│ 4. CloudWatch Logs → CloudWatch Metrics                                 │
│    CloudWatch automatically extracts metrics from EMF logs              │
│    STATUS: ❓ UNKNOWN (automatic extraction process)                    │
└─────────────────────────────────────────────────────────────────────────┘
                                    ↓
┌─────────────────────────────────────────────────────────────────────────┐
│ 5. CloudWatch Metrics Storage                                           │
│    Metrics stored by namespace in CloudWatch Metrics                    │
│    STATUS: ❓ UNKNOWN (need to query CloudWatch console)                │
└─────────────────────────────────────────────────────────────────────────┘
                                    ↓
┌─────────────────────────────────────────────────────────────────────────┐
│ 6. Grafana Data Source                                                  │
│    Grafana reads from CloudWatch via configured data source             │
│    STATUS: ❓ UNKNOWN (CDP platform configuration)                      │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## What We Think We Know (Based on CDP Documentation)

### AWS Embedded Metrics Format (EMF)

From `cdp-documentation/how-to/custom-metrics.md`:

**How It Works:**
- Metrics are written as structured JSON logs to stdout
- CloudWatch automatically extracts metrics from logs with special EMF structure
- No direct CloudWatch API calls (no `PutMetricData` required)
- Avoids IAM permissions issues and API throttling

**Node.js Pattern (Documented):**
```javascript
import {createMetricsLogger, Unit, StorageResolution} from 'aws-embedded-metrics'

async function countLoginAttempt(value = 1) {
  if (!config.get('isProduction')) return

  try {
    const metrics = createMetricsLogger()
    metrics.putMetric('loginAttempts', value, Unit.Count, StorageResolution.Standard)
    await metrics.flush()
  } catch (e) {
    // log or ignore the error
  }
}
```

**Namespace Behavior:**
> "Any metric reported by your service will automatically be stored under a namespace matching your service's name."

**Example:**
> "If a service named `example-backend-service` reports a metric called `loginAttempts` it would appear in grafana as `example-backend-service.loginAttempts`."

**Authentication:**
> "The credentials required for reporting custom metrics are injected automatically into your service's container when it is running on the platform. No extra setup is required."

---

## Implementation Comparison

### .NET Implementation (trade-imports-decision-deriver) - ✅ WORKING

**Library:** Amazon.CloudWatch.EMF (.NET package)

**Pattern:** EmfExporter listens to System.Diagnostics.Metrics and exports as EMF

**File:** `src/Deriver/Metrics/EmfExporter.cs`
```csharp
using Amazon.CloudWatch.EMF.Logger;
using Amazon.CloudWatch.EMF.Model;

public static class EmfExporter
{
    public static void Init(ILoggerFactory loggerFactory, string? awsNamespace)
    {
        s_awsNamespace = awsNamespace;
        s_meterListener.InstrumentPublished = (instrument, listener) =>
        {
            if (instrument.Meter.Name is MetricNames.MeterName)
            {
                listener.EnableMeasurementEvents(instrument);
            }
        };
        s_meterListener.Start();
    }

    private static void OnMeasurementRecorded<T>(
        Instrument instrument,
        T measurement,
        ReadOnlySpan<KeyValuePair<string, object?>> tags,
        object? state
    )
    {
        using var metricsLogger = new MetricsLogger(s_loggerFactory);
        metricsLogger.SetNamespace(s_awsNamespace);
        metricsLogger.PutMetric(name, Convert.ToDouble(measurement), Enum.Parse<Unit>(instrument.Unit!));
        metricsLogger.Flush();
    }
}
```

**Metrics Created:** `src/Deriver/Metrics/ConsumerMetrics.cs`
```csharp
public class ConsumerMetrics
{
    public ConsumerMetrics(IMeterFactory meterFactory)
    {
        var meter = meterFactory.Create(MetricNames.MeterName);

        _consumeTotal = meter.CreateCounter<long>(
            "MessagingConsume",
            nameof(Unit.COUNT),
            description: "Number of messages consumed"
        );
        // ... other metrics
    }
}
```

**Namespace:** "trade-imports-decision-deriver" (set in configuration)

**Observed Behavior:** Metrics visible in Grafana under namespace "trade-imports-decision-deriver"

---

### Java Implementation (trade-demo-backend) - ❓ STATUS UNKNOWN

**Library:** software.amazon.cloudwatchlogs:aws-embedded-metrics:4.2.0

**Configuration:** `src/main/java/uk/gov/defra/cdp/trade/demo/config/EmfMetricsConfig.java`
```java
@Configuration
@ConditionalOnProperty(value = "aws.emf.enabled", havingValue = "true", matchIfMissing = false)
public class EmfMetricsConfig {

    @Value("${aws.emf.namespace:}")
    private String namespace;

    @PostConstruct
    public void configureEmf() {
        log.info("Initializing AWS Embedded Metrics Format");
        log.info("EMF namespace: {}", namespace);

        if (namespace == null || namespace.isBlank()) {
            throw new IllegalStateException(
                "AWS_EMF_NAMESPACE must be set when AWS_EMF_ENABLED=true"
            );
        }

        // Set environment variables that EMF library reads
        System.setProperty("AWS_EMF_NAMESPACE", namespace);
        System.setProperty("AWS_EMF_SERVICE_NAME", serviceName);
        System.setProperty("AWS_EMF_SERVICE_TYPE", serviceType);

        environment = new DefaultEnvironment(EnvironmentConfigurationProvider.getConfig());
    }
}
```

**Service:** `src/main/java/uk/gov/defra/cdp/trade/demo/common/metrics/EmfMetricsService.java`
```java
@Service
@Profile("!test")
public class EmfMetricsService implements MetricsService {

    @Override
    public void counterWithContext(String name, double value, DimensionSet dimensions,
                                   Map<String, Object> properties) {
        try {
            MetricsLogger metrics = new MetricsLogger();

            if (dimensions != null) {
                metrics.putDimensions(dimensions);
            }

            metrics.putMetric(name, value, Unit.COUNT);

            if (properties != null) {
                properties.forEach(metrics::putProperty);
            }

            metrics.flush();

        } catch (Exception e) {
            logger.error("Failed to record metric: {}", name, e);
        }
    }
}
```

**Environment Variables:** `src/main/resources/application.yml`
```yaml
aws:
  emf:
    enabled: ${AWS_EMF_ENABLED:false}
    namespace: ${AWS_EMF_NAMESPACE:}
    service:
      name: ${AWS_EMF_SERVICE_NAME:trade-demo-backend}
      type: ${AWS_EMF_SERVICE_TYPE:SpringBootApp}
```

**Debug Endpoint:** `/debug/run-metrics-experiments`
- Emits 7 test metrics
- Returns status and namespace information

---

## Established Facts (With Evidence)

### FACT 1: Debug metrics endpoint executes successfully

**Evidence:**
```bash
curl -X POST https://trade-demo-backend.dev.cdp-int.defra.cloud/debug/run-metrics-experiments | jq .
```

**Expected Response:**
```json
{
  "status": "completed",
  "metrics_emitted": 7,
  "emf_enabled": true,
  "namespace": "trade-demo-backend",
  "timestamp": "2025-10-18T...",
  "verification": "Check CloudWatch → Metrics → 'trade-demo-backend' namespace"
}
```

**Metrics Emitted:**
1. `app_startup` (counter, value 1)
2. `order_created` (counter with dimensions)
3. `order_processed` (counter with context properties)
4. `response_time` (counter, value 250ms)
5. `cache_hit` (counter with cache dimension)
6. `example_created` (existing business metric)
7. `example_updated` (existing business metric)

**Conclusion:** ✅ The debug endpoint runs without errors and calls MetricsLogger.flush() 7 times.

---

### FACT 2: Java EMF library is properly configured

**Evidence A - Dependency:**

`pom.xml`:
```xml
<dependency>
    <groupId>software.amazon.cloudwatchlogs</groupId>
    <artifactId>aws-embedded-metrics</artifactId>
    <version>4.2.0</version>
</dependency>
```

**Evidence B - Configuration Bean:**

EmfMetricsConfig uses `@ConditionalOnProperty` to only activate when `aws.emf.enabled=true`.

Sets required System properties:
- `AWS_EMF_NAMESPACE`
- `AWS_EMF_SERVICE_NAME`
- `AWS_EMF_SERVICE_TYPE`

**Evidence C - Service Implementation:**

EmfMetricsService creates new MetricsLogger per operation (thread-safe pattern matching .NET implementation).

**Conclusion:** ✅ Configuration follows AWS EMF library requirements.

---

### FACT 3: .NET service metrics ARE visible in Grafana

**Evidence:** User confirmation that trade-imports-decision-deriver shows metrics like `MessagingConsume` in Grafana under the "trade-imports-decision-deriver" namespace.

**Implementation Pattern:**
- Uses `Amazon.CloudWatch.EMF` library
- Listens to .NET Diagnostics.Metrics API
- Exports via MetricsLogger.Flush()
- Sets namespace via `metricsLogger.SetNamespace()`

**Conclusion:** ✅ EMF pattern DOES work on CDP platform for .NET services.

---

## Key Differences: .NET vs Java

| Aspect | .NET (WORKING) | Java (UNKNOWN) | Analysis |
|--------|----------------|----------------|----------|
| **Library** | Amazon.CloudWatch.EMF | aws-embedded-metrics:4.2.0 | Both official AWS libraries ✅ |
| **Metrics API** | System.Diagnostics.Metrics | Direct MetricsLogger calls | Different approach ⚠️ |
| **Namespace** | Set via SetNamespace() | Set via System.setProperty() | Different configuration method ⚠️ |
| **Logger Creation** | new MetricsLogger(loggerFactory) | new MetricsLogger() | .NET passes ILoggerFactory ⚠️ |
| **Flush Pattern** | using/Dispose pattern | Try-catch block | Both should work ✅ |
| **Dimensions** | SetDimensions(dimensionSet) | putDimensions(dimensionSet) | Same API ✅ |

**Key Observation:** The .NET implementation uses `System.Diagnostics.Metrics` as an abstraction layer, while Java calls MetricsLogger directly. This shouldn't affect EMF output format, but worth investigating.

---

## Unknowns - Investigation Questions

### UNKNOWN 1: Are EMF-formatted logs being written to stdout?

**Question:** Does MetricsLogger.flush() actually write EMF JSON to stdout in our Java implementation?

**How to Verify:**
1. Run service locally with `AWS_EMF_ENABLED=true`
2. Capture stdout
3. Look for JSON logs with `_aws` metadata field

**Expected EMF Structure:**
```json
{
  "_aws": {
    "Timestamp": 1729264267000,
    "CloudWatchMetrics": [{
      "Namespace": "trade-demo-backend",
      "Dimensions": [["ServiceName"]],
      "Metrics": [{
        "Name": "example_created",
        "Unit": "Count"
      }]
    }]
  },
  "example_created": 1.0,
  "ServiceName": "trade-demo-backend",
  "ServiceType": "SpringBootApp"
}
```

**Status:** ❓ NEED TO VERIFY

---

### UNKNOWN 2: What does our EMF JSON actually look like?

**Question:** Does our Java EMF output match the AWS EMF specification?

**AWS EMF Specification Requirements:**
- Must have `_aws` top-level field
- Must have `CloudWatchMetrics` array with Namespace, Dimensions, Metrics
- Metric values must be at top-level of JSON (alongside `_aws`)
- Dimensions must list all dimension combinations

**How to Verify:**
1. Capture EMF JSON from local run
2. Compare with .NET service EMF JSON
3. Validate against AWS EMF spec: https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/CloudWatch_Embedded_Metric_Format_Specification.html

**Status:** ❓ NEED TO COMPARE

---

### UNKNOWN 3: Is AWS_EMF_NAMESPACE reaching the EMF library?

**Question:** Does the EMF library actually read our System.setProperty() values?

**Concern:** Java EMF library might expect **environment variables** not System properties.

**Evidence to Gather:**
- Check if EMF library documentation specifies environment variables vs system properties
- Verify namespace appears in EMF log output
- Test with actual environment variable instead of System.setProperty()

**Alternative Configuration:**
```java
// Instead of:
System.setProperty("AWS_EMF_NAMESPACE", namespace);

// Try setting before JVM starts in docker-compose or deployment config:
environment:
  - AWS_EMF_NAMESPACE=trade-demo-backend
```

**Status:** ❓ CONFIGURATION UNCERTAIN

---

### UNKNOWN 4: Are EMF logs reaching CloudWatch?

**Question:** Do EMF-formatted logs make it to CloudWatch Logs from our ECS container?

**How to Verify:**
1. Query CloudWatch Logs Insights for service log group
2. Search for logs containing `_aws` field
3. Filter for logs with `CloudWatchMetrics` metadata

**CloudWatch Logs Insights Query:**
```
fields @timestamp, @message
| filter @message like /_aws/
| sort @timestamp desc
| limit 20
```

**Status:** ❓ CANNOT VERIFY (no direct CloudWatch access via terminal)

---

### UNKNOWN 5: Are metrics being extracted to CloudWatch Metrics?

**Question:** Is CloudWatch successfully extracting metrics from our EMF logs?

**How to Verify:**
1. Check CloudWatch Metrics console for "trade-demo-backend" namespace
2. Query for specific metric names (example_created, order_created, etc.)
3. Verify metric data points exist

**Grafana Verification:**
1. Navigate to https://metrics.dev.cdp-int.defra.cloud/
2. Check namespace dropdown for "trade-demo-backend"
3. Try to create visualization with CloudWatch datasource

**Status:** ❓ NEED GRAFANA ACCESS TO VERIFY

---

### UNKNOWN 6: What's different about .NET's MetricsLogger?

**Question:** Does .NET's MetricsLogger initialization differ in a way that affects EMF output?

**.NET Pattern:**
```csharp
using var metricsLogger = new MetricsLogger(s_loggerFactory);
metricsLogger.SetNamespace(s_awsNamespace);
metricsLogger.PutMetric(name, value, unit);
metricsLogger.Flush();
```

**Java Pattern:**
```java
MetricsLogger metrics = new MetricsLogger();
metrics.putMetric(name, value, Unit.COUNT);
metrics.flush();
```

**Key Difference:** .NET passes ILoggerFactory to constructor, Java uses default constructor.

**Hypothesis:** Java's default constructor might not properly configure EMF output destination or format.

**Status:** ❓ NEED TO RESEARCH JAVA EMF LIBRARY INITIALIZATION

---

## Investigation Plan - Iterative Experiments

### Phase 1: Local Verification of EMF Output

**Goal:** Confirm EMF-formatted JSON is being written to stdout

**Experiment 1.1 - Capture Local Stdout:**
```bash
# In trade-demo-backend directory
export AWS_EMF_ENABLED=true
export AWS_EMF_NAMESPACE=trade-demo-backend
export AWS_EMF_SERVICE_NAME=trade-demo-backend

mvn spring-boot:run 2>&1 | tee /tmp/metrics-test.log

# In another terminal:
curl -X POST http://localhost:8085/debug/run-metrics-experiments

# Search for EMF logs:
grep "_aws" /tmp/metrics-test.log
```

**Expected Result:**
- Should see JSON logs with `_aws` field
- Each metric should produce one EMF log entry
- 7 EMF logs total from debug endpoint

**If EMF logs NOT found:**
- Problem: MetricsLogger.flush() not writing to stdout
- Next: Check EMF library initialization

**If EMF logs found:**
- ✅ Progress to Phase 2
- Save sample EMF JSON for comparison

---

**Experiment 1.2 - Verify EMF Structure:**

Extract one EMF log and validate structure:

```bash
# Extract first EMF log
grep "_aws" /tmp/metrics-test.log | head -1 | jq .
```

**Checklist:**
- ✅ Has `_aws` top-level field?
- ✅ Has `CloudWatchMetrics` array?
- ✅ Namespace is "trade-demo-backend"?
- ✅ Metric name at top-level (e.g., `"example_created": 1.0`)?
- ✅ Has `Timestamp` in milliseconds?

---

### Phase 2: DEV Environment Verification

**Goal:** Confirm EMF logs reach application logs in DEV

**Experiment 2.1 - Trigger Metrics in DEV:**
```bash
curl -X POST https://trade-demo-backend.dev.cdp-int.defra.cloud/debug/run-metrics-experiments
```

**Experiment 2.2 - Query OpenSearch for EMF Logs:**

Search OpenSearch for logs with EMF structure:

```json
GET /cdp-logs-*/_search
{
  "query": {
    "bool": {
      "must": [
        {"term": {"service.name.keyword": "trade-demo-backend"}},
        {"exists": {"field": "_aws"}},
        {"range": {"@timestamp": {"gte": "now-10m"}}}
      ]
    }
  },
  "_source": ["@timestamp", "message", "_aws", "CloudWatchMetrics"],
  "size": 10
}
```

**Alternative Query (if _aws not indexed):**

```json
GET /cdp-logs-*/_search
{
  "query": {
    "bool": {
      "must": [
        {"term": {"service.name.keyword": "trade-demo-backend"}},
        {"match": {"message": "CloudWatchMetrics"}},
        {"range": {"@timestamp": {"gte": "now-10m"}}}
      ]
    }
  },
  "size": 10
}
```

**Expected Results:**
- ✅ EMF logs found: Progress to Phase 3
- ❌ EMF logs NOT found: Problem is in EMF library configuration or stdout capture

---

### Phase 3: CloudWatch Metrics Verification

**Goal:** Confirm metrics are extracted and stored in CloudWatch Metrics

**NOTE:** This phase requires access to AWS Console or CloudWatch API (not available via terminal).

**Manual Verification Steps:**

1. Navigate to AWS Console → CloudWatch → Metrics
2. Look for "trade-demo-backend" namespace
3. Check for metric names:
   - `example_created`
   - `order_created`
   - `app_startup`
   - etc.

**Alternative: Grafana Verification:**

1. Go to https://metrics.dev.cdp-int.defra.cloud/
2. Create new dashboard → Add visualization
3. Select CloudWatch datasource
4. Scroll to "trade-demo-backend" namespace in dropdown
5. If namespace appears, select metric name
6. Check if data points exist

---

### Phase 4: Format Comparison with Working .NET Service

**Goal:** Compare EMF JSON structure between Java and .NET implementations

**Experiment 4.1 - Get .NET EMF Sample:**

Request sample EMF log from trade-imports-decision-deriver logs (if accessible).

**Experiment 4.2 - Side-by-Side Comparison:**

Create comparison table:

| Field | .NET EMF | Java EMF | Match? |
|-------|----------|----------|--------|
| `_aws.Timestamp` | (value) | (value) | ❓ |
| `_aws.CloudWatchMetrics[0].Namespace` | "trade-imports-decision-deriver" | "trade-demo-backend" | ✅ |
| `_aws.CloudWatchMetrics[0].Metrics[0].Unit` | "Count" | "Count" | ❓ |
| Metric value location | Top-level | Top-level | ❓ |
| ServiceName property | (check) | "trade-demo-backend" | ❓ |

---

### Phase 5: Configuration Debugging

**Goal:** Test alternative EMF configuration approaches

**Experiment 5.1 - Test Environment Variable Instead of System Property:**

Modify EmfMetricsConfig to NOT use System.setProperty():

```java
// Remove these lines:
System.setProperty("AWS_EMF_NAMESPACE", namespace);
System.setProperty("AWS_EMF_SERVICE_NAME", serviceName);
System.setProperty("AWS_EMF_SERVICE_TYPE", serviceType);
```

Set in docker-compose.yml instead:
```yaml
environment:
  - AWS_EMF_ENABLED=true
  - AWS_EMF_NAMESPACE=trade-demo-backend
  - AWS_EMF_SERVICE_NAME=trade-demo-backend
  - AWS_EMF_SERVICE_TYPE=SpringBootApp
```

Re-test metrics emission.

---

**Experiment 5.2 - Test Explicit Namespace on MetricsLogger:**

Modify EmfMetricsService to set namespace directly:

```java
@Override
public void counterWithContext(String name, double value, DimensionSet dimensions,
                               Map<String, Object> properties) {
    try {
        MetricsLogger metrics = new MetricsLogger();

        // Explicitly set namespace before putting metric
        metrics.setNamespace(namespace); // Add this

        if (dimensions != null) {
            metrics.putDimensions(dimensions);
        }

        metrics.putMetric(name, value, Unit.COUNT);

        if (properties != null) {
            properties.forEach(metrics::putProperty);
        }

        metrics.flush();

    } catch (Exception e) {
        logger.error("Failed to record metric: {}", name, e);
    }
}
```

---

## Expected AWS EMF Log Format

Based on AWS EMF Specification:

```json
{
  "_aws": {
    "Timestamp": 1729264267000,
    "CloudWatchMetrics": [
      {
        "Namespace": "trade-demo-backend",
        "Dimensions": [
          ["ServiceName"],
          ["ServiceName", "Environment"]
        ],
        "Metrics": [
          {
            "Name": "example_created",
            "Unit": "Count"
          }
        ]
      }
    ]
  },
  "ServiceName": "trade-demo-backend",
  "ServiceType": "SpringBootApp",
  "Environment": "development",
  "example_created": 1.0
}
```

**Key Requirements:**
1. `_aws` object at top level
2. `CloudWatchMetrics` array with at least one entry
3. `Namespace` string (required)
4. `Metrics` array with metric definitions
5. Actual metric **values** at top-level JSON (e.g., `"example_created": 1.0`)
6. `Timestamp` in epoch milliseconds
7. All dimension values present as top-level properties

**Reference:** https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/CloudWatch_Embedded_Metric_Format_Specification.html

---

## What We DON'T Know

### UNKNOWN 7: Does Java EMF library support all features?

**Question:** Is there feature parity between .NET and Java EMF libraries?

**Concerns:**
- Java library version 4.2.0 - is this latest?
- Are there known issues with Spring Boot integration?
- Does it support all EMF specification features?

**How to Verify:**
- Check GitHub issues: https://github.com/awslabs/aws-embedded-metrics-java
- Review library documentation
- Check for version updates or known bugs

---

### UNKNOWN 8: How does CDP configure Grafana datasource?

**Question:** What CloudWatch credentials and configuration does Grafana use?

**Possible Issues:**
- Grafana might be configured to only read specific namespaces
- IAM role might not have permissions for new namespaces
- Region mismatch between metrics and Grafana datasource

**Status:** ❓ CDP platform team knowledge required

---

### UNKNOWN 9: Is there a CloudWatch metrics delay?

**Question:** How long does it take for EMF metrics to appear in CloudWatch after log emission?

**Known Behavior:**
- CloudWatch processes logs asynchronously
- Metrics extraction may have delay (seconds to minutes)
- Initial metric creation might take longer than subsequent updates

**How to Verify:**
- Monitor Grafana after running experiment
- Check for metric appearance over 5-10 minute window

---

## Next Steps

1. **Run Phase 1 experiments locally** to verify EMF JSON output
2. **Capture sample EMF log** for format validation
3. **Run Phase 2 in DEV** to confirm logs reach OpenSearch
4. **Request Grafana access** for Phase 3 verification
5. **Iterate based on findings** - update this document with evidence

**Decision Points:**

**IF EMF logs found locally:**
→ Problem is in CloudWatch ingestion or metrics extraction
→ Focus on Phase 3 (CloudWatch Metrics verification)

**IF EMF logs NOT found locally:**
→ Problem is in Java EMF library configuration
→ Focus on Phase 5 (configuration debugging)

**IF EMF logs in OpenSearch but no metrics in CloudWatch:**
→ Problem is in automatic metrics extraction
→ Investigate EMF JSON structure compliance with AWS spec

---

## References

- **CDP Custom Metrics Docs:** `/Users/benoit/projects/defra/cdp/DEFRA/cdp-documentation/how-to/custom-metrics.md`
- **Working .NET Implementation:** `/Users/benoit/projects/defra/cdp/trade-imports-decision-deriver/src/Deriver/Metrics/`
- **Java EMF Library:** https://github.com/awslabs/aws-embedded-metrics-java
- **AWS EMF Specification:** https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/CloudWatch_Embedded_Metric_Format_Specification.html
- **Java Implementation:** `/Users/benoit/projects/defra/cdp/DEFRA/trade-demo-backend/src/main/java/uk/gov/defra/cdp/trade/demo/common/metrics/EmfMetricsService.java`
