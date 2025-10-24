package uk.gov.defra.cdp.trade.demo.common.metrics;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import software.amazon.cloudwatchlogs.emf.logger.MetricsLogger;
import software.amazon.cloudwatchlogs.emf.model.DimensionSet;
import software.amazon.cloudwatchlogs.emf.model.Unit;

import java.util.Map;

/**
 * Production implementation of MetricsService using AWS Embedded Metrics Format.
 * <p>
 * Active in all profiles EXCEPT "test".
 * <p>
 * HOW IT WORKS:
 * - Creates MetricsLogger per metric operation (thread-safe pattern)
 * - Writes structured JSON logs to stdout
 * - CloudWatch automatically extracts metrics from logs
 * - No CloudWatch API calls (no PutMetricData errors)
 * - Properties are queryable in CloudWatch Logs Insights
 * <p>
 * ERROR HANDLING:
 * - All operations wrapped in try-catch
 * - Errors logged with full stack traces but never thrown (silent failure)
 * - Application continues on metric failures
 * <p>
 * THREAD SAFETY:
 * - Creates new MetricsLogger instance per operation (no shared state)
 * - Safe to use from multiple threads concurrently
 */
@Service
@Slf4j
@Profile("!integration-test")
public class EmfMetricsService implements MetricsService {

    @Override
    public void counter(String name) {
        counter(name, 1.0);
    }

    @Override
    public void counter(String name, double value) {
        counter(name, value, null);
    }

    @Override
    public void counter(String name, double value, DimensionSet dimensions) {
        counterWithContext(name, value, dimensions, null);
    }

    @Override
    public void counterWithContext(String name, double value, DimensionSet dimensions,
                                   Map<String, Object> properties) {
        try {
            MetricsLogger metrics = new MetricsLogger();

            // Add dimensions if provided
            if (dimensions != null) {
                metrics.putDimensions(dimensions);
            }

            // Add metric
            metrics.putMetric(name, value, Unit.COUNT);

            // Add properties if provided
            if (properties != null) {
                properties.forEach(metrics::putProperty);
            }

            // Flush writes structured JSON log
            metrics.flush();

        } catch (Exception e) {
            // Never crash application due to metrics errors
            // Log full exception with stack trace for debugging
            log.error("Failed to record metric: {}", name, e);
        }
    }
}
