# CDP Compliance Requirements - trade-demo-backend

This document describes how `trade-demo-backend` implements CDP (Core Delivery Platform) requirements.

## 1. Container Platform Requirements

**CDP Requirement:** Service must build as single Docker container, Linux x86 architecture, with curl and /bin/sh available. Non-root user required.

**Implementation:** Multi-stage Dockerfile with production target meeting all requirements.

**Source:**
- Container: `Dockerfile:64-93`
- Architecture: `amazoncorretto:21-alpine` (Linux x86)
- curl installation: `Dockerfile:71`
- Non-root user: `Dockerfile:77`
- HEALTHCHECK instruction: `Dockerfile:88-89`
- ENTRYPOINT with no parameters: `Dockerfile:93`

---

## 2. Health Check Endpoint

**CDP Requirement:** Endpoint accessible at /health, responding with HTTP 200 OK in under 5 seconds. No database connectivity tests.

**Implementation:** Spring Boot Actuator with MongoDB health indicator explicitly disabled. Base path overridden to serve /health instead of /actuator/health.

**Source:**
- Endpoint configuration: `src/main/resources/application.yml:56-70`
- MongoDB health disabled: `src/main/resources/application.yml:69-70`
- HEALTHCHECK in Dockerfile: `Dockerfile:88-89`

```yaml
management:
  endpoints:
    web:
      base-path: /
      exposure:
        include: health
  health:
    mongo:
      enabled: false  # Prevents database connectivity tests
```

---

## 3. Port Configuration

**CDP Requirement:** Service listens on PORT environment variable (default: 8085).

**Implementation:** Spring Boot server.port configuration reads from PORT environment variable with 8085 default.

**Source:**
- Port configuration: `src/main/resources/application.yml:2`
- Dockerfile EXPOSE: `Dockerfile:80`

```yaml
server:
  port: ${PORT:8085}
```

---

## 4. Logging Format (ECS JSON)

**CDP Requirement:** JSON output to stdout only. Application sets log.level, message, http.request.method, http.response.status_code, url.full. Platform auto-populates trace.id, service.name, service.version, @timestamp. No PII logging.

**Implementation:** Logback with co.elastic.logging:logback-ecs-encoder. MDC populated by RequestTracingFilter. NestedErrorEcsEncoder transforms flat error.* fields to nested error {} objects for CDP Data Prepper compatibility.

**Source:**
- Logback configuration: `src/main/resources/logback-spring.xml:10-19`
- ECS encoder: `logback-spring.xml:14-18`
- Nested error encoder: `src/main/java/uk/gov/defra/cdp/trade/demo/logging/NestedErrorEcsEncoder.java`
- MDC population: `src/main/java/uk/gov/defra/cdp/trade/demo/filter/RequestTracingFilter.java:38-56`
- Health check filtering: `src/main/java/uk/gov/defra/cdp/trade/demo/logging/HealthCheckFilter.java`

```xml
<appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
    <filter class="uk.gov.defra.cdp.trade.demo.logging.HealthCheckFilter"/>
    <encoder class="co.elastic.logging.logback.EcsEncoder">
        <serviceName>${spring.application.name}</serviceName>
        <serviceVersion>${SERVICE_VERSION}</serviceVersion>
    </encoder>
</appender>
```

**Platform-Reserved Fields:** Application does NOT set trace.id, service.name, service.version, @timestamp. CDP ingestion pipeline populates these automatically.

---

## 5. Request Tracing (x-cdp-request-id)

**CDP Requirement:** Extract x-cdp-request-id header from incoming requests. Include trace ID in all log entries. Propagate trace ID to outbound HTTP calls. Clear MDC after request to prevent thread pool contamination.

**Implementation:** RequestTracingFilter (HIGHEST_PRECEDENCE) extracts header and populates MDC. TraceIdPropagationInterceptor adds header to outbound RestClient/RestTemplate calls. MDC cleared in finally block.

**Source:**
- Filter implementation: `src/main/java/uk/gov/defra/cdp/trade/demo/filter/RequestTracingFilter.java:21-62`
- Header extraction: `RequestTracingFilter.java:40-43`
- MDC population: `RequestTracingFilter.java:42,46-47`
- MDC cleanup: `RequestTracingFilter.java:58-60`
- Outbound propagation: `src/main/java/uk/gov/defra/cdp/trade/demo/interceptor/TraceIdPropagationInterceptor.java`

```java
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestTracingFilter implements Filter {
    private static final String TRACE_ID_HEADER = "x-cdp-request-id";
    private static final String MDC_TRACE_ID = "trace.id";

    public void doFilter(...) {
        try {
            String traceId = httpRequest.getHeader(TRACE_ID_HEADER);
            if (traceId != null && !traceId.isBlank()) {
                MDC.put(MDC_TRACE_ID, traceId);
            }
            MDC.put(MDC_HTTP_METHOD, httpRequest.getMethod());
            MDC.put(MDC_URL_FULL, httpRequest.getRequestURL().toString());

            chain.doFilter(request, response);

            MDC.put(MDC_HTTP_STATUS, String.valueOf(httpResponse.getStatus()));
        } finally {
            MDC.clear();  // Critical: prevents thread pool contamination
        }
    }
}
```

---

## 6. Configuration Management

**CDP Requirement:** All configuration via environment variables. Fail-fast on missing required configuration. No secrets in code or configuration files.

**Implementation:** Spring Boot @Value annotations with environment variable defaults. EmfMetricsConfig validates AWS_EMF_NAMESPACE at startup with IllegalStateException if missing when enabled.

**Source:**
- Environment variable binding: `src/main/resources/application.yml` (all ${VAR:default} patterns)
- Fail-fast validation: `src/main/java/uk/gov/defra/cdp/trade/demo/config/EmfMetricsConfig.java:62-66`
- MongoDB URI: `application.yml:10`
- Port configuration: `application.yml:2`

```yaml
server:
  port: ${PORT:8085}
spring:
  data:
    mongodb:
      uri: ${MONGO_URI:mongodb://localhost:27017}
```

---

## 7. Certificate Management

**CDP Requirement:** Scan TRUSTSTORE_* environment variables. Load base64-encoded PEM certificates. Create SSLContext combining JVM defaults with CDP certificates.

**Implementation:** TrustStoreConfiguration creates custom SSLContext at HIGHEST_PRECEDENCE. CertificateLoader scans environment for TRUSTSTORE_* variables. CombinedTrustManager delegates to both default and custom trust managers.

**Source:**
- SSLContext bean: `src/main/java/uk/gov/defra/cdp/trade/demo/config/tls/TrustStoreConfiguration.java:52-77`
- Certificate loading: `src/main/java/uk/gov/defra/cdp/trade/demo/config/tls/CertificateLoader.java`
- Environment scanning: `src/main/java/uk/gov/defra/cdp/trade/demo/config/tls/TrustStoreEnvironmentScanner.java`
- Combined trust manager: `TrustStoreConfiguration.java:139-186`

```java
@Configuration
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TrustStoreConfiguration {
    @Bean
    public SSLContext customSslContext() {
        List<CertificateEntry> customCerts = certificateLoader.loadCustomCertificates();
        X509TrustManager combinedTrustManager = createCombinedTrustManager(customCerts);

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, new TrustManager[]{combinedTrustManager}, new SecureRandom());
        return sslContext;
    }
}
```

---

## 8. MongoDB IAM Authentication

**CDP Requirement:** IAM authentication (authMechanism=MONGODB-AWS). TLS/SSL enabled. Custom CA certificates supported. Connection pool configured. Graceful shutdown.

**Implementation:** MongoConfig extends AbstractMongoClientConfiguration. Connection string with authMechanism=MONGODB-AWS. Custom SSLContext injected via constructor. Connection pool settings applied. @PreDestroy closes client cleanly.

**Source:**
- Configuration class: `src/main/java/uk/gov/defra/cdp/trade/demo/config/MongoConfig.java:35-184`
- Connection string: `MongoConfig.java:98`
- SSL configuration: `MongoConfig.java:117-125`
- Connection pool: `MongoConfig.java:112-114,130-140`
- Graceful shutdown: `MongoConfig.java:172-183`
- URI format: `application.yml:10`

```java
@Configuration
public class MongoConfig extends AbstractMongoClientConfiguration {
    private final SSLContext customSslContext;

    public MongoConfig(SSLContext customSslContext) {
        this.customSslContext = customSslContext;
    }

    protected void configureClientSettings(MongoClientSettings.Builder builder) {
        ConnectionString connectionString = new ConnectionString(mongoUri);
        builder.applyConnectionString(connectionString);

        if (sslEnabled) {
            builder.applyToSslSettings(sslSettings -> {
                sslSettings.enabled(true);
                sslSettings.context(customSslContext);
            });
        }
    }

    @PreDestroy
    public void destroy() {
        if (mongoClientInstance != null) {
            mongoClientInstance.close();
        }
    }
}
```

**Connection String Format:** `mongodb://host:port/database?authMechanism=MONGODB-AWS&authSource=$external`

---

## 9. HTTP Proxy Support

**CDP Requirement:** HTTP_PROXY environment variable respected. RestTemplate/RestClient configured with proxy. Custom SSL certificates configured for HTTP clients.

**Implementation:** ProxyConfig reads HTTP_PROXY at startup, sets Java system properties (http.proxyHost, http.proxyPort, https.proxyHost, https.proxyPort), configures default ProxySelector. RestClientConfig creates RestClient.Builder with custom SSLContext.

**Source:**
- Proxy configuration: `src/main/java/uk/gov/defra/cdp/trade/demo/config/ProxyConfig.java:39-79`
- System properties: `ProxyConfig.java:63-66`
- ProxySelector: `ProxyConfig.java:69-71`
- RestClient configuration: `src/main/java/uk/gov/defra/cdp/trade/demo/config/RestClientConfig.java`

```java
@Configuration
public class ProxyConfig {
    @PostConstruct
    public void configureProxy() {
        String httpProxy = System.getenv("HTTP_PROXY");
        if (httpProxy == null || httpProxy.isEmpty()) return;

        URI proxyUri = URI.create(httpProxy);
        String proxyHost = proxyUri.getHost();
        int proxyPort = proxyUri.getPort();

        System.setProperty("http.proxyHost", proxyHost);
        System.setProperty("http.proxyPort", String.valueOf(proxyPort));
        System.setProperty("https.proxyHost", proxyHost);
        System.setProperty("https.proxyPort", String.valueOf(proxyPort));

        ProxySelector.setDefault(ProxySelector.of(
            new InetSocketAddress(proxyHost, proxyPort)
        ));
    }
}
```

---

## 10. Graceful Shutdown

**CDP Requirement:** Complete in-flight requests before shutdown. Close database connections cleanly. 30 second timeout.

**Implementation:** Spring Boot server.shutdown=graceful with 30 second timeout. MongoConfig @PreDestroy closes MongoClient. EmfMetricsConfig @PreDestroy flushes metrics with 10 second timeout.

**Source:**
- Graceful shutdown: `src/main/resources/application.yml:3`
- Shutdown timeout: `application.yml:21`
- MongoDB cleanup: `src/main/java/uk/gov/defra/cdp/trade/demo/config/MongoConfig.java:172-183`
- EMF cleanup: `src/main/java/uk/gov/defra/cdp/trade/demo/config/EmfMetricsConfig.java:80-92`

```yaml
server:
  shutdown: graceful

spring:
  lifecycle:
    timeout-per-shutdown-phase: 30s
```

---

## 11. Metrics (AWS Embedded Metrics Format)

**CDP Requirement:** Custom metrics shipped to CloudWatch Metrics via CloudWatch Agent sidecar. EMF library must be explicitly configured with ECS mode to connect to sidecar on TCP port 25888. Namespace must match service name.

**Critical Discovery:** EMF metrics are NOT written to stdout on CDP. Instead:
1. EMF library is explicitly set to ECS mode via `AWS_EMF_ENVIRONMENT=ECS`
2. Opens TCP connection to CloudWatch Agent sidecar on port 25888
3. CloudWatch Agent sidecar ships metrics to CloudWatch Metrics
4. Grafana queries CloudWatch Metrics (NOT OpenSearch logs)

**Implementation:** EmfMetricsConfig validates configuration at startup. Environment variables must be set by CDP platform BEFORE JVM starts (cdp-app-config repository). EmfMetricsService creates MetricsLogger per operation for thread safety.

**Source:**
- EMF validation: `src/main/java/uk/gov/defra/cdp/trade/demo/config/EmfMetricsConfig.java:65-84`
- Metrics service: `src/main/java/uk/gov/defra/cdp/trade/demo/common/metrics/EmfMetricsService.java`
- Application configuration: `src/main/resources/application.yml:35-41`
- Platform configuration: `cdp-app-config/services/trade-demo-backend/dev/trade-demo-backend.env`

**Environment Variables (set in cdp-app-config):**
```bash
AWS_EMF_ENABLED=true
AWS_EMF_ENVIRONMENT=ECS  # CRITICAL: Forces TCP connection to CloudWatch Agent on port 25888
AWS_EMF_NAMESPACE=trade-demo-backend
AWS_EMF_SERVICE_NAME=trade-demo-backend
AWS_EMF_SERVICE_TYPE=SpringBootApp
```

```java
@Configuration
@ConditionalOnProperty(value = "aws.emf.enabled", havingValue = "true")
public class EmfMetricsConfig {
    @PostConstruct
    public void configureEmf() {
        // Validates configuration - does NOT set environment variables
        if (namespace == null || namespace.isBlank()) {
            throw new IllegalStateException(
                "AWS_EMF_NAMESPACE must be set when AWS_EMF_ENABLED=true. " +
                "This must be configured as an environment variable before JVM starts."
            );
        }

        // Create environment instance - library has already read config
        environment = new DefaultEnvironment(EnvironmentConfigurationProvider.getConfig());
    }
}
```

**How Metrics Flow on CDP:**
```
Application → EMF Library → TCP :25888 → CloudWatch Agent Sidecar → CloudWatch Metrics → Grafana
```

**Key Facts:**
- ✅ MUST explicitly set `AWS_EMF_ENVIRONMENT=ECS` in cdp-app-config
- ✅ ECS mode forces EMF to connect to CloudWatch Agent sidecar on TCP port 25888
- ✅ Environment variables must be set in cdp-app-config BEFORE deployment
- ❌ DO NOT use System.setProperty() - EMF reads config at static initialization time
- ❌ DO NOT set AWS_EMF_ENVIRONMENT=Local - forces stdout mode (breaks metrics on CDP)
- ❌ EMF JSON does NOT appear in application logs or OpenSearch (uses TCP, not stdout)

**Investigation:** See `METRICS_INVESTIGATION.md` for detailed analysis confirming this behavior.

---

## 12. Spring Boot Actuator Security

**CDP Requirement:** Only /health endpoint exposed in production. Sensitive actuator endpoints disabled.

**Implementation:** Spring Boot Actuator exposure restricted to health endpoint only. Base path set to / instead of /actuator. show-details=never prevents information disclosure.

**Source:**
- Endpoint exposure: `src/main/resources/application.yml:56-64`
- Base path override: `application.yml:59`
- Health details: `application.yml:64`

```yaml
management:
  endpoints:
    web:
      base-path: /
      exposure:
        include: health  # Only health exposed, all others disabled
  endpoint:
    health:
      show-details: never  # Prevents information disclosure
```

**Development Environment:** Development profile (`application-dev.yml`) may enable additional endpoints for debugging.

---

## Verification Tests

**Source:** Integration tests verify all CDP requirements:
- Health endpoint: `src/test/java/uk/gov/defra/cdp/trade/demo/health/HealthCheckIT.java`
- Request tracing: `src/test/java/uk/gov/defra/cdp/trade/demo/filter/RequestTracingFilterTest.java`
- MongoDB IAM: `src/test/java/uk/gov/defra/cdp/trade/demo/config/MongoConfigIT.java`
- Certificate loading: `src/test/java/uk/gov/defra/cdp/trade/demo/config/tls/TrustStoreConfigurationTest.java`
- Compliance verification: `src/test/java/uk/gov/defra/cdp/trade/demo/ExampleComplianceIT.java`

---

**Document Version:** 1.0
**Service:** trade-demo-backend
**Spring Boot:** 3.2.11
**Java:** 21 (Amazon Corretto)
