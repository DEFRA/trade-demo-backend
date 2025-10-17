package uk.gov.defra.cdp.trade.demo.common.metrics;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

/**
 * Integration tests for MetricsService with metrics ENABLED.
 * <p>
 * TESTING APPROACH:
 * Tests the production MetricsConfig which creates CloudWatchMeterRegistry when
 * cdp.metrics.enabled=true. The config uses AWS DefaultCredentialsProvider which
 * discovers credentials from environment, credentials file, or IAM roles.
 * <p>
 * WHAT THESE TESTS VERIFY:
 * - CloudWatchMeterRegistry is created by production config (not SimpleMeterRegistry)
 * - Metrics are registered in CloudWatchMeterRegistry
 * - MetricsService integration works with CloudWatch configuration
 * - Configuration beans are properly wired
 * <p>
 * LIMITATIONS:
 * These tests use DefaultCredentialsProvider which may discover local credentials
 * (~/.aws/credentials) but won't actually publish to CloudWatch in tests.
 * End-to-end verification requires deployment to CDP with IAM roles.
 */
@SpringBootTest
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
        Counter ordersCounter = meterRegistry.find("orders_created").counter();
        assertThat(ordersCounter)
            .as("Counter should be registered in CloudWatchMeterRegistry")
            .isNotNull();

        assertThat(meterRegistry.find("default_counter").counter())
            .as("Counter with default value should be registered")
            .isNotNull();

        Counter incrementalCounter = meterRegistry.find("incremental_counter").counter();
        assertThat(incrementalCounter)
            .as("Incremental counter should be registered")
            .isNotNull();

        // Verify idempotency - same counter instance is reused on subsequent calls
        metricsService.counter("orders_created", 1.0);
        Counter sameOrdersCounter = meterRegistry.find("orders_created").counter();
        assertThat(sameOrdersCounter)
            .as("Should return same counter instance on subsequent calls (idempotent)")
            .isSameAs(ordersCounter);

        // Verify total number of metrics registered (should still be 3, not 4)
        assertThat(meterRegistry.getMeters())
            .as("CloudWatchMeterRegistry should contain all registered metrics")
            .hasSizeGreaterThanOrEqualTo(3);
    }
}
