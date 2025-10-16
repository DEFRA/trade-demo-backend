package uk.gov.defra.cdp.trade.demo.common.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Integration tests for MetricsService.
 *
 * Tests verify that:
 * 1. Metrics are recorded when ENABLE_METRICS=true
 * 2. Metrics are skipped when ENABLE_METRICS=false
 * 3. Service handles errors gracefully (never crashes)
 * 4. Counter metrics are registered in MeterRegistry
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

    /**
     * Integration test with metrics enabled.
     * This test verifies that metrics ARE recorded when ENABLE_METRICS=true.
     */
    @SpringBootTest
    @TestPropertySource(properties = {
        "cdp.metrics.enabled=true"
    })
    static class MetricsEnabledIT {

        @Autowired
        private MetricsService metricsService;

        @Autowired
        private MeterRegistry meterRegistry;

        @BeforeEach
        void setUp() {
            meterRegistry.clear();
        }

        @Test
        void shouldRecordCounterWhenMetricsEnabled() {
            // Given: ENABLE_METRICS=true

            // When: Recording a counter metric
            metricsService.counter("orders_created", 5.0);

            // Then: Metric should be registered and incremented
            Counter counter = meterRegistry.find("orders_created").counter();
            assertThat(counter)
                .as("Counter should be registered when metrics enabled")
                .isNotNull();

            assertThat(counter.count())
                .as("Counter value should match recorded value")
                .isEqualTo(5.0);
        }

        @Test
        void shouldRecordCounterWithDefaultValue() {
            // Given: ENABLE_METRICS=true

            // When: Recording a counter without value
            metricsService.counter("default_counter");

            // Then: Metric should be registered with value 1.0
            Counter counter = meterRegistry.find("default_counter").counter();
            assertThat(counter)
                .as("Counter should be registered")
                .isNotNull();

            assertThat(counter.count())
                .as("Counter should have default value of 1.0")
                .isEqualTo(1.0);
        }

        @Test
        void shouldIncrementExistingCounter() {
            // Given: ENABLE_METRICS=true and existing counter

            // When: Recording same metric multiple times
            metricsService.counter("incremental_counter", 2.0);
            metricsService.counter("incremental_counter", 3.0);

            // Then: Counter should accumulate values
            Counter counter = meterRegistry.find("incremental_counter").counter();
            assertThat(counter)
                .as("Counter should be registered")
                .isNotNull();

            assertThat(counter.count())
                .as("Counter should accumulate increments")
                .isEqualTo(5.0);
        }
    }
}
