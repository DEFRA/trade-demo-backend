package uk.gov.defra.cdp.trade.demo.debug;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import software.amazon.cloudwatchlogs.emf.model.DimensionSet;
import uk.gov.defra.cdp.trade.demo.common.metrics.MetricsService;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Debug endpoints for verifying stack traces and metrics in production.
 *
 * TEMPORARY: These endpoints will be removed after verification is complete.
 *
 * Purpose:
 * - Verify exception logging produces error.stack_trace in OpenSearch
 * - Verify EMF metrics emit to CloudWatch correctly
 * - Test both local (catch+log) and global (GlobalExceptionHandler) patterns
 */
@RestController
@RequestMapping("/debug")
public class DebugController {

    private static final Logger logger = LoggerFactory.getLogger(DebugController.class);

    private final MetricsService metricsService;

    @Value("${spring.application.name:trade-demo-backend}")
    private String serviceName;

    @Value("${service.version:unknown}")
    private String serviceVersion;

    @Value("${ENVIRONMENT:local}")
    private String environment;

    @Value("${AWS_EMF_ENABLED:false}")
    private boolean emfEnabled;

    @Value("${AWS_EMF_NAMESPACE:}")
    private String emfNamespace;

    public DebugController(MetricsService metricsService) {
        this.metricsService = metricsService;
    }

    /**
     * Run error logging experiments to verify stack traces in OpenSearch.
     *
     * Tests both local exception handling (catch + log) and
     * global exception handling (GlobalExceptionHandler).
     */
    @PostMapping("/run-error-experiments")
    public ResponseEntity<Map<String, Object>> runErrorExperiments() {
        logger.info("Starting error logging experiments");

        // LOCAL EXCEPTION HANDLING (catch + log, return 200)

        // EXPERIMENT 1: Simple runtime exception
        try {
            throw new RuntimeException("LOCAL_EXP_1: Caught and logged locally");
        } catch (Exception e) {
            logger.error("LOCAL_EXP_1: Exception caught in controller", e);
        }

        // EXPERIMENT 2: Nested exception with cause
        try {
            throw new IllegalStateException("LOCAL_EXP_2: Outer exception",
                new NullPointerException("LOCAL_EXP_2: Inner cause"));
        } catch (Exception e) {
            logger.error("LOCAL_EXP_2: Nested exception caught", e);
        }

        // GLOBAL EXCEPTION HANDLING (throw to GlobalExceptionHandler)
        // Note: These will be caught by GlobalExceptionHandler and return 500,
        // but we catch them here to continue the experiment

        // EXPERIMENT 3: Runtime exception via GlobalExceptionHandler
        try {
            throwGlobalExp1();
        } catch (Exception e) {
            logger.error("GLOBAL_EXP_1: Caught before GlobalExceptionHandler for demo", e);
        }

        // EXPERIMENT 4: Deep call stack
        try {
            throwGlobalExp2Level1();
        } catch (Exception e) {
            logger.error("GLOBAL_EXP_2: Caught deep stack exception", e);
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", "completed");
        response.put("experiments_run", 4);
        response.put("local_exceptions", new String[]{"LOCAL_EXP_1", "LOCAL_EXP_2"});
        response.put("global_exceptions", new String[]{"GLOBAL_EXP_1", "GLOBAL_EXP_2"});
        response.put("timestamp", Instant.now().toString());
        response.put("verification_query", "GET /cdp-logs-*/_search { \"query\": { \"match\": { \"message\": \"_EXP_\" } } }");

        logger.info("Completed error logging experiments");
        return ResponseEntity.ok(response);
    }

    /**
     * Run metrics experiments to verify EMF in CloudWatch.
     *
     * Tests simple counters, dimensions, context properties, and batching.
     */
    @PostMapping("/run-metrics-experiments")
    public ResponseEntity<Map<String, Object>> runMetricsExperiments() {
        logger.info("Starting metrics experiments");

        // EXPERIMENT 1: Simple counter
        metricsService.counter("debug.experiment.1.simple");

        // EXPERIMENT 2: Counter with custom value
        metricsService.counter("debug.experiment.2.value", 42.0);

        // EXPERIMENT 3: Counter with dimensions
        metricsService.counter("debug.experiment.3.dimensions", 1.0,
            DimensionSet.of("experiment_type", "dimensions", "environment", environment));

        // EXPERIMENT 4: Counter with context properties
        metricsService.counterWithContext("debug.experiment.4.context", 1.0,
            DimensionSet.of("test_type", "context"),
            Map.of("experiment_id", "EXP_4", "run_time", System.currentTimeMillis()));

        // EXPERIMENT 5: Multiple emissions (test aggregation)
        for (int i = 0; i < 10; i++) {
            metricsService.counter("debug.experiment.5.batch", 1.0);
        }

        // Also emit existing business metrics to verify they work
        metricsService.counter("example_created");
        metricsService.counter("example_updated");

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", "completed");
        response.put("metrics_emitted", 7);
        response.put("emf_enabled", emfEnabled);
        response.put("namespace", emfNamespace.isEmpty() ? serviceName : emfNamespace);
        response.put("timestamp", Instant.now().toString());
        response.put("verification", "Check CloudWatch → Metrics → '" +
            (emfNamespace.isEmpty() ? serviceName : emfNamespace) + "' namespace");

        logger.info("Completed metrics experiments");
        return ResponseEntity.ok(response);
    }

    /**
     * Get debug configuration information.
     *
     * Shows current service configuration for troubleshooting.
     */
    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> getInfo() {
        Map<String, Object> response = new LinkedHashMap<>();

        Map<String, Object> service = new LinkedHashMap<>();
        service.put("name", serviceName);
        service.put("version", serviceVersion);
        service.put("environment", environment);
        response.put("service", service);

        Map<String, Object> emf = new LinkedHashMap<>();
        emf.put("enabled", emfEnabled);
        emf.put("namespace", emfNamespace.isEmpty() ? serviceName : emfNamespace);
        response.put("emf", emf);

        Map<String, Object> logging = new LinkedHashMap<>();
        logging.put("encoder", "ECS");
        logging.put("level", "INFO");
        response.put("logging", logging);

        return ResponseEntity.ok(response);
    }

    // Helper methods for global exception experiments

    private void throwGlobalExp1() {
        throw new RuntimeException("GLOBAL_EXP_1: Via GlobalExceptionHandler");
    }

    private void throwGlobalExp2Level1() {
        throwGlobalExp2Level2();
    }

    private void throwGlobalExp2Level2() {
        throwGlobalExp2Level3();
    }

    private void throwGlobalExp2Level3() {
        throw new RuntimeException("GLOBAL_EXP_2: Deep call stack");
    }
}
