package uk.gov.defra.cdp.trade.demo.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import software.amazon.cloudwatchlogs.emf.model.DimensionSet;

import java.util.Map;
import uk.gov.defra.cdp.trade.demo.common.metrics.MetricsService;
import uk.gov.defra.cdp.trade.demo.common.metrics.NoOpMetricsService;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test verifying MetricsService is wired correctly.
 * <p>
 * In test profile, expects NoOpMetricsService to be injected.
 * This test verifies Spring profile switching and dependency injection work correctly.
 */
class MetricsServiceIT extends IntegrationBase {

    @Autowired
    private MetricsService metricsService;

    @Test
    void shouldInjectNoOpMetricsServiceInTestProfile() {
        // Given: Test profile active

        // Then: NoOpMetricsService should be injected
        assertThat(metricsService)
            .as("Should inject NoOpMetricsService in test profile")
            .isInstanceOf(NoOpMetricsService.class);
    }

    @Test
    void shouldNotThrowWhenRecordingSimpleCounter() {
        // All metrics operations should be silent no-ops
        metricsService.counter("test_metric");
        metricsService.counter("test_metric", 5.0);

        // No exceptions should be thrown
    }

    @Test
    void shouldNotThrowWhenRecordingCounterWithDimensions() {
        metricsService.counter("test_metric", 5.0,
            DimensionSet.of("type", "test"));

        // No exceptions should be thrown
    }

    @Test
    void shouldNotThrowWhenRecordingCounterWithContext() {
        metricsService.counterWithContext("test_metric", 5.0,
            DimensionSet.of("type", "test"),
            Map.of("orderId", "123", "customerId", "CUST-789"));

        // No exceptions should be thrown
    }
}
