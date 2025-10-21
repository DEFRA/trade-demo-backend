# CRITICAL FINDING: Java EMF Library Not Writing to Stdout

**Date:** 2025-10-18
**Test:** Local EMF output capture
**Status:** 🔴 ROOT CAUSE IDENTIFIED

---

> **⚠️ HISTORICAL DOCUMENT - INCORRECT ASSUMPTIONS**
>
> This document represents our initial investigation based on the INCORRECT assumption that EMF should write to stdout on CDP.
>
> **ACTUAL SOLUTION DISCOVERED (2025-10-20):**
> - EMF on CDP uses **TCP connection to CloudWatch Agent sidecar** on port 25888
> - Must explicitly set `AWS_EMF_ENVIRONMENT=ECS` in cdp-app-config (auto-detection unreliable)
> - ECS mode forces TCP Agent connection, NOT stdout
> - See `CDP-COMPLIANCE.md` Section 11 for correct implementation
>
> This document is preserved for historical reference showing the investigation process.

---

## The Problem

The Java AWS Embedded Metrics library (`aws-embedded-metrics:4.2.0`) is **NOT writing EMF JSON to stdout**. Instead, it's trying to send metrics to a CloudWatch agent endpoint.

---

## Evidence

### Test Execution

**Command:**
```bash
export AWS_EMF_ENABLED=true
export AWS_EMF_NAMESPACE=trade-demo-backend
mvn spring-boot:run
curl -X POST http://localhost:8085/debug/run-metrics-experiments
```

**Result:** ❌ Zero EMF logs in stdout

### Key Log Messages

```
{"log.level": "INFO","message":"AWS Embedded Metrics Format initialized successfully"}
{"log.level": "INFO","message":"EMF namespace: trade-demo-backend"}
{"log.level": "INFO","message":"Starting metrics experiments"}
{"log.level": "INFO","message":"Endpoint is not defined. Using default: tcp://127.0.0.1:25888"}
{"log.level": "INFO","message":"Completed metrics experiments"}
```

**SMOKING GUN:**
> "Endpoint is not defined. Using default: tcp://127.0.0.1:25888"

The EMF library is in **Agent mode**, sending to a CloudWatch agent TCP endpoint, NOT writing to stdout!

---

## Root Cause Analysis

### EMF Library Behavior

The Java EMF library has multiple "environments" for outputting metrics:

1. **Agent Mode** (default): Sends to local CloudWatch agent at `tcp://127.0.0.1:25888`
2. **Lambda Mode**: Writes to stdout (for AWS Lambda)
3. **ECS Mode**: Writes to stdout (for ECS Fargate)
4. **EC2 Mode**: Sends to CloudWatch agent

**Our Issue:** The library is defaulting to **Agent Mode** instead of **ECS/stdout mode**.

### Why This Happens

From `software.amazon.cloudwatchlogs.emf.environment.AgentBasedEnvironment`:

The library auto-detects the environment. When it doesn't detect ECS metadata, it falls back to Agent mode.

**Expected:** In ECS containers, EMF library should detect `ECS_CONTAINER_METADATA_URI` environment variable and use stdout.

**Actual:** Locally (and possibly in our ECS container), it's not detecting ECS environment and defaults to Agent mode.

---

## Comparison with .NET Implementation

### .NET Service (trade-imports-decision-deriver) - WORKING

The .NET implementation uses:
```csharp
using var metricsLogger = new MetricsLogger(s_loggerFactory);
metricsLogger.SetNamespace(s_awsNamespace);
metricsLogger.PutMetric(name, value, unit);
metricsLogger.Flush();
```

The .NET library (`Amazon.CloudWatch.EMF`) **writes to stdout by default** when using `MetricsLogger`.

### Java Service (trade-demo-backend) - NOT WORKING

```java
MetricsLogger metrics = new MetricsLogger();
metrics.putMetric(name, value, Unit.COUNT);
metrics.flush();
```

The Java library (`aws-embedded-metrics:4.2.0`) **does NOT write to stdout by default** - it sends to CloudWatch agent.

---

## The Fix

### Option 1: Force Lambda Environment (Quick Fix)

Set environment variable to force stdout output:

```java
System.setProperty("AWS_EXECUTION_ENV", "AWS_Lambda_java21");
```

Or in `application.yml`:
```yaml
aws:
  emf:
    enabled: true
    namespace: trade-demo-backend
    # Force Lambda mode to write to stdout
    environment: Lambda
```

Then in `EmfMetricsConfig`:
```java
@PostConstruct
public void configureEmf() {
    // Force Lambda environment to write to stdout
    System.setProperty("AWS_EXECUTION_ENV", "AWS_Lambda_java21");
    System.setProperty("AWS_EMF_NAMESPACE", namespace);
    // ...
}
```

### Option 2: Use Specific Environment Configuration

Configure EMF to use `EnvironmentProvider`:

```java
import software.amazon.cloudwatchlogs.emf.config.Configuration;
import software.amazon.cloudwatchlogs.emf.config.SystemWrapper;
import software.amazon.cloudwatchlogs.emf.environment.EnvironmentProvider;

@PostConstruct
public void configureEmf() {
    log.info("Initializing AWS Embedded Metrics Format");

    // Create configuration that forces stdout
    Configuration config = new Configuration();
    config.setServiceName(serviceName);
    config.setServiceType(serviceType);
    config.setLogGroupName("/aws/ecs/" + namespace);

    // Force environment to use stdout sink
    System.setProperty("AWS_EMF_ENVIRONMENT", "Local"); // Forces stdout
    System.setProperty("AWS_EMF_NAMESPACE", namespace);

    environment = EnvironmentProvider.getEnvironment(config, new SystemWrapper());
}
```

### Option 3: Manual JSON Construction (Most Reliable)

Build EMF JSON manually and write to stdout/logger:

```java
@Service
@Profile("!test")
public class EmfMetricsService implements MetricsService {

    private static final Logger emfLogger = LoggerFactory.getLogger("EMF");

    @Value("${aws.emf.namespace}")
    private String namespace;

    @Override
    public void counterWithContext(String name, double value, DimensionSet dimensions,
                                   Map<String, Object> properties) {
        try {
            // Build EMF JSON manually
            Map<String, Object> emfLog = new LinkedHashMap<>();

            // Add _aws metadata
            Map<String, Object> aws = new LinkedHashMap<>();
            aws.put("Timestamp", System.currentTimeMillis());

            Map<String, Object> cwMetrics = new LinkedHashMap<>();
            cwMetrics.put("Namespace", namespace);
            cwMetrics.put("Dimensions", buildDimensions(dimensions));
            cwMetrics.put("Metrics", List.of(Map.of(
                "Name", name,
                "Unit", "Count"
            )));

            aws.put("CloudWatchMetrics", List.of(cwMetrics));
            emfLog.put("_aws", aws);

            // Add metric value at top level
            emfLog.put(name, value);

            // Add dimensions as properties
            if (dimensions != null) {
                dimensions.getDimensionKeys().forEach(key ->
                    emfLog.put(key, dimensions.getDimensionValue(key))
                );
            }

            // Add additional properties
            if (properties != null) {
                emfLog.putAll(properties);
            }

            // Write to stdout via logger
            String json = objectMapper.writeValueAsString(emfLog);
            emfLogger.info(json); // This goes to stdout

        } catch (Exception e) {
            logger.error("Failed to record metric: {}", name, e);
        }
    }
}
```

---

## Recommended Solution

**For CDP Compatibility:** Use **Option 1** (Force Lambda environment) as the simplest fix that uses the official library.

This forces the EMF library to write to stdout, which ECS will forward to CloudWatch Logs, where CloudWatch Metrics will extract the metrics.

---

## Next Steps

1. ✅ **Identified root cause**: Java EMF library using Agent mode instead of stdout
2. ⏭️ **Test Option 1**: Force Lambda environment and verify EMF JSON in stdout
3. ⏭️ **Deploy to DEV**: Verify metrics appear in CloudWatch and Grafana
4. ⏭️ **Compare with .NET**: Confirm .NET uses stdout by default (if we can resolve build issues)

---

## Files

- **Test script:** `/Users/benoit/projects/defra/cdp/test-java-emf-output.sh`
- **Full logs:** `/tmp/java-emf-output.log`
- **EMF output:** `/tmp/java-emf-only.json` (empty - confirms the issue)

---

## References

- **Java EMF Library:** https://github.com/awslabs/aws-embedded-metrics-java
- **Environment Detection:** `software.amazon.cloudwatchlogs.emf.environment.EnvironmentProvider`
- **Agent vs Lambda modes:** https://github.com/awslabs/aws-embedded-metrics-java/blob/main/docs/environment.md
