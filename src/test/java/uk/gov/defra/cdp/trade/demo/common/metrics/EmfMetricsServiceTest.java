package uk.gov.defra.cdp.trade.demo.common.metrics;

import org.junit.jupiter.api.Test;
import software.amazon.cloudwatchlogs.emf.model.DimensionSet;

import java.util.Map;

/**
 * Unit test for EmfMetricsService.
 * <p>
 * Since EMF writes to logs asynchronously, we only verify that:
 * 1. Methods don't throw exceptions
 * 2. Error handling works (tested via code coverage)
 * <p>
 * Full EMF functionality is tested via integration tests in deployed environments
 * by verifying metrics appear in CloudWatch and logs contain EMF JSON.
 */
class EmfMetricsServiceTest {

    private final EmfMetricsService metricsService = new EmfMetricsService();

    @Test
    void shouldNotThrowWhenRecordingSimpleCounter() {
        metricsService.counter("test_metric");
        metricsService.counter("test_metric", 5.0);
    }

    @Test
    void shouldNotThrowWhenRecordingCounterWithDimensions() {
        metricsService.counter("test_metric", 5.0,
            DimensionSet.of("dimension1", "value1", "dimension2", "value2"));
    }

    @Test
    void shouldNotThrowWhenRecordingCounterWithContext() {
        metricsService.counterWithContext("test_metric", 5.0,
            DimensionSet.of("type", "test"),
            Map.of(
                "orderId", "12345",
                "customerId", "CUST-789",
                "amount", 99.99
            ));
    }

    @Test
    void shouldNotThrowWhenDimensionsAreNull() {
        metricsService.counter("test_metric", 5.0, null);
    }

    @Test
    void shouldNotThrowWhenPropertiesAreNull() {
        metricsService.counterWithContext("test_metric", 5.0,
            DimensionSet.of("type", "test"),
            null);
    }

    @Test
    void shouldNotThrowWhenBothDimensionsAndPropertiesAreNull() {
        metricsService.counterWithContext("test_metric", 5.0, null, null);
    }
}
