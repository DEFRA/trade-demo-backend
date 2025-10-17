package uk.gov.defra.cdp.trade.demo.common.metrics;

import software.amazon.cloudwatchlogs.emf.model.DimensionSet;

import java.util.Map;

/**
 * Service for recording custom business metrics using AWS Embedded Metrics Format.
 * <p>
 * This interface provides a testable abstraction over AWS EMF library.
 * Production implementation writes structured JSON logs that CloudWatch
 * automatically extracts into metrics.
 * <p>
 * Test implementation is a no-op to avoid metrics overhead in tests.
 */
public interface MetricsService {

    /**
     * Record a counter metric with value 1.
     *
     * @param name Metric name (e.g., "orders.created")
     */
    void counter(String name);

    /**
     * Record a counter metric with custom value.
     *
     * @param name Metric name
     * @param value Metric value
     */
    void counter(String name, double value);

    /**
     * Record a counter metric with dimensions.
     *
     * @param name Metric name
     * @param value Metric value
     * @param dimensions CloudWatch dimensions for filtering/grouping
     */
    void counter(String name, double value, DimensionSet dimensions);

    /**
     * Record a metric with context properties.
     * Properties are searchable in CloudWatch Logs Insights but not sent to CloudWatch Metrics.
     *
     * @param name Metric name
     * @param value Metric value
     * @param dimensions CloudWatch dimensions
     * @param properties Context properties (orderId, customerId, etc.)
     */
    void counterWithContext(String name, double value, DimensionSet dimensions,
                           Map<String, Object> properties);
}
