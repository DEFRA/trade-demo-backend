package uk.gov.defra.cdp.trade.demo.common.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Integration tests for MetricsService with metrics DISABLED (default).
 * <p>
 * Tests verify that:
 * 1. Metrics are skipped when ENABLE_METRICS=false (default)
 * 2. Service handles errors gracefully (never crashes)
 * 3. Metrics are not registered in MeterRegistry when disabled
 * <p>
 * For tests with metrics ENABLED, see MetricsServiceEnabledIT.java
 */
@SpringBootTest
class MetricsServiceIT {

    @Autowired
    private MetricsService metricsService;

    @Autowired
    private MeterRegistry meterRegistry;

    @BeforeEach
    void setUp() {
        // Clear metrics before each test to avoid interference
        meterRegistry.clear();
    }

    @Test
    void shouldRecordCounterWhenMetricsDisabled() {
        // Given: ENABLE_METRICS defaults to false in test environment

        // When: Recording a counter metric
        metricsService.counter("test_counter", 5.0);

        // Then: Metric should not be registered (metrics disabled)
        Counter counter = meterRegistry.find("test_counter").counter();
        assertThat(counter)
            .as("Counter should not be registered when metrics disabled")
            .isNull();
    }

    @Test
    void shouldRecordCounterWithDefaultValue() {
        // Given: ENABLE_METRICS=false (default)

        // When: Recording a counter without value
        metricsService.counter("test_counter_default");

        // Then: Should not crash (graceful handling when disabled)
        assertThatCode(() -> metricsService.counter("test_counter_default"))
            .as("Should handle disabled metrics gracefully")
            .doesNotThrowAnyException();
    }

    @Test
    void shouldHandleInvalidMetricNameGracefully() {
        // Given: A potentially problematic metric name

        // When: Recording metric with special characters
        metricsService.counter("test.metric-with_special/chars", 1.0);

        // Then: Should not crash (error handling)
        assertThatCode(() -> metricsService.counter("test.metric-with_special/chars", 1.0))
            .as("Should handle any metric name without crashing")
            .doesNotThrowAnyException();
    }

    @Test
    void shouldHandleNegativeValuesGracefully() {
        // Given: A negative counter value (not typical but should not crash)

        // When: Recording negative value
        metricsService.counter("test_negative", -1.0);

        // Then: Should not crash
        assertThatCode(() -> metricsService.counter("test_negative", -1.0))
            .as("Should handle negative values without crashing")
            .doesNotThrowAnyException();
    }
}
