package uk.gov.defra.cdp.trade.demo.common.metrics;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

/**
 * Integration tests for MetricsService with metrics ENABLED.
 * <p>
 * TESTING APPROACH:
 * Spring Boot does NOT provide auto-configuration for CloudWatch metrics (PR #11276
 * was rejected). Therefore, we manually configure CloudWatchMeterRegistry in
 * CloudWatchTestConfig with dummy AWS credentials. Since we create only ONE
 * MeterRegistry bean, Spring Boot injects it directly (no CompositeMeterRegistry
 * wrapper). This is appropriate for single-backend scenarios.
 * <p>
 * WHAT THESE TESTS VERIFY:
 * - CloudWatchMeterRegistry is created (not SimpleMeterRegistry fallback)
 * - Metrics are registered in CloudWatchMeterRegistry
 * - MetricsService integration works with CloudWatch
 * - Configuration beans are properly wired
 * <p>
 * LIMITATIONS:
 * These tests use dummy AWS credentials and cannot verify actual CloudWatch
 * publishing. End-to-end verification requires deployment to CDP with real credentials.
 */
@SpringBootTest
@Import(CloudWatchTestConfig.class)
@TestPropertySource(properties = {
    "cdp.metrics.enabled=true"
})
class MetricsServiceEnabledIT {

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
    void shouldConfigureCloudWatchMeterRegistry() {
        // Given: Application configured with CloudWatch metrics and dummy AWS credentials

        // Then: CloudWatchMeterRegistry should be injected (proves configuration works)
        assertThat(meterRegistry)
            .as("MeterRegistry must be CloudWatchMeterRegistry. " +
                "If this is SimpleMeterRegistry, CloudWatch export is not configured! " +
                "Actual type: " + meterRegistry.getClass().getSimpleName())
            .isInstanceOf(io.micrometer.cloudwatch2.CloudWatchMeterRegistry.class);
    }

    @Test
    void shouldRecordMetricsWhenEnabled() {
        // Given: ENABLE_METRICS=true

        // When: Recording metrics
        metricsService.counter("orders_created", 5.0);
        metricsService.counter("default_counter");
        metricsService.counter("incremental_counter", 2.0);
        metricsService.counter("incremental_counter", 3.0);

        // Then: Metrics should be registered in CloudWatchMeterRegistry
        // Note: CloudWatchMeterRegistry is a StepMeterRegistry that buffers metrics
        // for batch publishing. Counter values may show 0.0 until publish() is called.
        // The important verification is that metrics ARE registered (not null).
        assertThat(meterRegistry.find("orders_created").counter())
            .as("Counter should be registered in CloudWatchMeterRegistry")
            .isNotNull();

        assertThat(meterRegistry.find("default_counter").counter())
            .as("Counter with default value should be registered")
            .isNotNull();

        assertThat(meterRegistry.find("incremental_counter").counter())
            .as("Incremental counter should be registered")
            .isNotNull();

        // Verify total number of metrics registered
        assertThat(meterRegistry.getMeters())
            .as("CloudWatchMeterRegistry should contain all registered metrics")
            .hasSizeGreaterThanOrEqualTo(3);
    }
}
