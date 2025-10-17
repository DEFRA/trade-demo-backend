package uk.gov.defra.cdp.trade.demo.common.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import uk.gov.defra.cdp.trade.demo.config.CdpConfig;

/**
 * Helper service for recording custom metrics to CloudWatch.
 *
 * CDP Platform requirement: Services should emit custom business metrics
 * to CloudWatch for monitoring and alerting.
 *
 * This service provides a simple API matching the Node.js/Python template pattern:
 * - counter(name, value) - Record a counter metric
 *
 * Features:
 * - Production only (checks ENABLE_METRICS environment variable)
 * - Graceful error handling (never crashes the application)
 * - Automatic namespace (service name) from Spring application name
 * - Standard resolution (1 minute) for cost efficiency
 *
 * Usage example:
 * <pre>
 * {@code
 * @Service
 * public class OrderService {
 *     private final MetricsService metricsService;
 *
 *     public void createOrder(Order order) {
 *         // ... business logic ...
 *         metricsService.counter("orders_created", 1);
 *     }
 * }
 * }
 * </pre>
 *
 * Implementation notes:
 * - Uses Spring Boot Micrometer (idiomatic Java approach)
 * - Directly exports to CloudWatch API (not EMF log parsing)
 * - Integrates with Spring Boot Actuator metrics
 * - Zero additional dependencies beyond spring-boot-starter-actuator
 */
@Service
public class MetricsService {

    private static final Logger logger = LoggerFactory.getLogger(MetricsService.class);

    private final MeterRegistry meterRegistry;
    private final CdpConfig cdpConfig;

    public MetricsService(MeterRegistry meterRegistry, CdpConfig cdpConfig) {
        this.meterRegistry = meterRegistry;
        this.cdpConfig = cdpConfig;
        logger.info("MetricsService initialized (enabled: {})", cdpConfig.getMetrics().isEnabled());
    }

    /**
     * Record a counter metric.
     * <p>
     * This method is thread-safe and idempotent. The counter is registered on first access
     * and reused on subsequent calls via MeterRegistry.counter(), which handles registration
     * internally and returns the same Counter instance for a given name.
     *
     * @param name  Metric name (e.g., "orders_created")
     * @param value Value to increment counter by (default 1)
     */
    public void counter(String name, double value) {
        if (!cdpConfig.getMetrics().isEnabled()) {
            logger.debug("Metrics disabled, skipping counter: {}", name);
            return;
        }

        try {
            // MeterRegistry.counter() is thread-safe and idempotent - returns existing counter
            // or creates new one atomically. This is the idiomatic Micrometer pattern.
            Counter counter = meterRegistry.counter(name,
                "description", "Custom business metric: " + name);

            counter.increment(value);
            logger.debug("Recorded counter metric: {} = {}", name, value);

        } catch (Exception e) {
            // Never crash the application due to metrics errors
            logger.error("Failed to record counter metric: {}. Error: {}", name, e.getMessage());
        }
    }

    /**
     * Record a counter metric with default value of 1.
     *
     * @param name Metric name (e.g., "orders_created")
     */
    public void counter(String name) {
        counter(name, 1.0);
    }
}
